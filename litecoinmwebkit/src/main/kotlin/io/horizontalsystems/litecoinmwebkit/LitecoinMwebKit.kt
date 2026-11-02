package io.horizontalsystems.litecoinmwebkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.RawOutput
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.litecoinkit.TestNetLitecoin
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.horizontalsystems.litecoinmwebkit.proto.Utxo
import io.horizontalsystems.litecoinmwebkit.storage.MwebAddressEntity
import io.horizontalsystems.litecoinmwebkit.storage.MwebDatabase
import io.horizontalsystems.litecoinmwebkit.storage.MwebSyncInfo
import io.horizontalsystems.litecoinmwebkit.storage.MwebTransaction
import io.horizontalsystems.litecoinmwebkit.storage.MwebUtxo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Litecoin MWEB wallet. One instance per wallet; the chain data lives in the
 * process-wide [MwebDaemon] and is shared between wallets, while outputs,
 * addresses and history are kept per wallet in a Room database that a
 * rescan never clears (MWEB has no spent-output history to replay).
 *
 * Receive addresses start at index 1; index 0 is the change address.
 */
class LitecoinMwebKit(
    private val context: Context,
    private val keychain: MwebKeychain,
    private val walletId: String,
    val networkType: LitecoinKit.NetworkType = LitecoinKit.NetworkType.MainNet,
    private val proxyUrl: String? = null,
    /** Blocks after which a received output counts as spendable; MWEB outputs are final after one. */
    private val confirmationsThreshold: Int = 1,
) {
    constructor(
        context: Context,
        seed: ByteArray,
        walletId: String,
        networkType: LitecoinKit.NetworkType = LitecoinKit.NetworkType.MainNet,
        proxyUrl: String? = null,
    ) : this(context, MwebKeychain.fromSeed(seed), walletId, networkType, proxyUrl)

    constructor(
        context: Context,
        words: List<String>,
        passphrase: String,
        walletId: String,
        networkType: LitecoinKit.NetworkType = LitecoinKit.NetworkType.MainNet,
        proxyUrl: String? = null,
    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, proxyUrl)

    private val tag = "LitecoinMwebKit"
    private val hrp = MwebAddress.hrp(networkType)
    private val chain = MwebDaemon.chainName(networkType)
    private val network = when (networkType) {
        LitecoinKit.NetworkType.MainNet -> MainNetLitecoin()
        LitecoinKit.NetworkType.TestNet -> TestNetLitecoin()
    }
    private val litecoinAddressConverter = AddressConverterChain().apply {
        prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
        prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
    }

    private val db = MwebDatabase.open(context, databaseName(networkType, walletId))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dbMutex = Mutex()
    private var rpc: MwebRpc? = null
    private var syncJob: Job? = null

    private val _syncState = MutableStateFlow<MwebSyncState>(MwebSyncState.NotSynced(IllegalStateException("not started")))
    val syncStateFlow: StateFlow<MwebSyncState> = _syncState.asStateFlow()
    val syncState: MwebSyncState get() = _syncState.value

    private val _lastBlockHeight = MutableStateFlow(0)
    val lastBlockHeightFlow: StateFlow<Int> = _lastBlockHeight.asStateFlow()

    val balanceFlow: Flow<MwebBalance> = db.utxos().allFlow().map { balance(it) }
    val balance: MwebBalance get() = balance(db.utxos().all())

    val transactionsFlow: Flow<List<MwebTransactionInfo>> = db.transactions().allFlow().map { list -> list.map { it.toInfo() } }

    val watchOnly: Boolean get() = keychain.spendSecret == null

    // region lifecycle

    fun start() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch { runSync() }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
        rpc?.shutdown()
        rpc = null
        _syncState.value = MwebSyncState.NotSynced(IllegalStateException("stopped"))
    }

    /** Restarts the daemon connection, e.g. after the device changed networks. */
    fun refresh() {
        stop()
        try {
            MwebDaemon.restart(context)
        } catch (e: MwebNativeException) {
            Log.w(tag, "daemon restart failed", e)
        }
        start()
    }

    fun onEnterForeground() {
        if (syncJob?.isActive != true) start()
    }

    fun onEnterBackground() {
        // The daemon keeps running: it is shared and cheap at the tip.
    }

    // endregion

    // region addresses

    /** The address at [index]; index 0 is the change address. */
    fun receiveAddress(index: Int): String {
        require(index >= 0)
        return addressAt(index)
    }

    /** The first receive address not yet used by a received output or a pending peg-in. */
    fun receiveAddress(): String = addressAt(nextReceiveIndex())

    /** Addresses of indices that received outputs, for display. */
    fun usedAddresses(): List<Pair<Int, String>> =
        db.utxos().all().map { it.addressIndex to it.address }.distinct().sortedBy { it.first }

    /** Throws [MwebAddressFormatException] unless [address] is an MWEB address of this network. */
    @Throws(MwebAddressFormatException::class)
    fun validateAddress(address: String) {
        MwebAddress.decode(address, hrp)
    }

    /** Throws unless [address] is an MWEB or a Litecoin address of this network (a Litecoin address means a peg-out). */
    @Throws(MwebAddressFormatException::class)
    fun validateDestination(address: String) {
        destinationScript(address)
    }

    private fun addressAt(index: Int): String {
        db.addresses().byIndex(index)?.let { return it.address }
        ensureAddresses(index + ADDRESS_GAP)
        return db.addresses().byIndex(index)?.address ?: error("address $index not derived")
    }

    private fun ensureAddresses(upToInclusive: Int) {
        val have = (db.addresses().maxIndex() ?: -1) + 1
        if (upToInclusive < have) return
        val derived = MwebNative.addresses(
            keychain.scanSecret.toHexString(), keychain.spendPubKey.toHexString(), chain, have, upToInclusive + 1
        ).split('\n').filter { it.isNotEmpty() }
        db.addresses().insertAll(derived.mapIndexed { i, address -> MwebAddressEntity(have + i, address) })
    }

    private fun addressIndexOf(address: String): Int? {
        db.addresses().byAddress(address)?.let { return it.index }
        val have = (db.addresses().maxIndex() ?: -1) + 1
        if (have >= ADDRESS_POOL_LIMIT) return null
        ensureAddresses(min(have + ADDRESS_POOL_LIMIT / 10, ADDRESS_POOL_LIMIT) - 1)
        return db.addresses().byAddress(address)?.index
    }

    // endregion

    // region balance and outputs

    private fun balance(utxos: List<MwebUtxo>): MwebBalance {
        val height = _lastBlockHeight.value
        var spendable = 0L
        var unconfirmed = 0L
        for (utxo in utxos) {
            if (utxo.spent) continue
            if (isConfirmed(utxo, height)) spendable += utxo.value else unconfirmed += utxo.value
        }
        return MwebBalance(spendable, unconfirmed)
    }

    private fun isConfirmed(utxo: MwebUtxo, lastBlockHeight: Int) =
        utxo.height > 0 && (confirmationsThreshold <= 1 || utxo.height <= lastBlockHeight - confirmationsThreshold + 1)

    fun unspentOutputs(): List<MwebUtxoInfo> {
        val height = _lastBlockHeight.value
        return db.utxos().unspent().filter { isConfirmed(it, height) }
            .map { MwebUtxoInfo(it.outputId, it.value, it.address, it.addressIndex, it.height, it.blockTime) }
    }

    // endregion

    // region sending

    /**
     * Fee in litoshi for sending [value] to [address] at [feeRate]
     * (litoshi per virtual byte, only relevant for the Litecoin output of a
     * peg-out). Throws [MwebSendError] when the balance does not cover it.
     */
    @Throws(MwebSendError::class)
    fun estimateFee(address: String, value: Long, feeRate: Int): Long =
        selectCoins(destinationScript(address), value, feeRate.toLong() * 1000).fee

    /** Largest value sendable to [address] at [feeRate] from the spendable balance. */
    fun maximumSpendableValue(address: String, feeRate: Int): Long {
        val script = destinationScript(address)
        val coins = db.utxos().unspent().filter { isConfirmed(it, _lastBlockHeight.value) }
        val total = coins.sumOf { it.value }
        val fee = MwebFee.estimate(listOf(TemplateOutput(script, total)), feeRate.toLong() * 1000)
        return max(0L, total - fee)
    }

    /**
     * Sends [value] litoshi to an MWEB address (MWEB to MWEB) or to a
     * Litecoin address (peg-out), paying the fee on top. Change returns to
     * address index 0. Returns the recorded transaction.
     */
    @Throws(MwebSendError::class)
    suspend fun send(address: String, value: Long, feeRate: Int): MwebTransactionInfo {
        val spendSecret = keychain.spendSecret ?: throw MwebSendError.WatchOnly()
        val rpc = this.rpc ?: throw MwebSendError.NotSynced()
        if (syncState !is MwebSyncState.Synced) throw MwebSendError.NotSynced()
        val script = destinationScript(address)
        val isPegOut = !MwebAddress.isMwebScript(script)
        if (isPegOut && value < LITECOIN_DUST) throw MwebSendError.DustAmount()
        val feeRatePerKb = feeRate.toLong() * 1000

        return dbMutex.withLock {
            val selection = selectCoins(script, value, feeRatePerKb)
            val template = MwebTxSerializer.serializeTemplate(
                selection.coins.map { TemplateInput(it.outputId.hexToByteArray(), it.addressIndex) },
                selection.outputs,
            )
            val created = try {
                rpc.create(template, keychain.scanSecret, spendSecret, feeRatePerKb, false)
            } catch (e: Exception) {
                throw MwebSendError.BroadcastRejected("Transaction creation failed: ${e.message}", e)
            }

            // Output IDs come back in template order: recipient (if MWEB), then change.
            val ownOutputIds = ArrayList<String>()
            var i = 0
            if (!isPegOut) i++
            if (selection.change > 0 && i < created.outputIdCount) ownOutputIds.add(created.getOutputId(i))

            val uid = UUID.randomUUID().toString()
            val tx = MwebTransaction(
                uid = uid,
                type = if (isPegOut) MwebTransaction.TYPE_PEG_OUT else MwebTransaction.TYPE_OUTGOING,
                amount = value,
                fee = selection.fee,
                timestamp = System.currentTimeMillis() / 1000,
                height = 0,
                status = MwebTransaction.STATUS_PENDING,
                toAddress = address,
                ownAddress = null,
                txHash = null,
                inputIds = selection.coins.joinToString(",") { it.outputId },
                outputIds = ownOutputIds.joinToString(","),
            )
            // Recorded before the broadcast so the row and the reservation of its inputs survive a
            // crash or an unknown broadcast result.
            db.transactions().insert(tx)
            for (coin in selection.coins) {
                db.utxos().update(coin.copy(spent = true, spentByTxUid = uid))
            }

            val txid = broadcast(rpc, created.rawTx.toByteArray(), uid)
            val sent = tx.copy(txHash = if (isPegOut) txid else null)
            db.transactions().update(sent)
            sent.toInfo()
        }
    }

    /**
     * Broadcasts and returns the txid. A rejection by the daemon marks the
     * row failed and releases its inputs; a transport failure leaves the
     * row pending, since the transaction may have gone out. Must be called
     * with [dbMutex] held.
     */
    private suspend fun broadcast(rpc: MwebRpc, rawTx: ByteArray, uid: String): String {
        try {
            return rpc.broadcast(rawTx)
        } catch (e: Exception) {
            if (isTransportFailure(e)) throw MwebSendError.BroadcastUnknown(e)
            markFailed(uid)
            throw MwebSendError.BroadcastRejected("Broadcast rejected: ${e.message}", e)
        }
    }

    private fun isTransportFailure(e: Exception): Boolean {
        val status = (e as? StatusRuntimeException)?.status ?: return false
        return when (status.code) {
            Status.Code.UNAVAILABLE, Status.Code.DEADLINE_EXCEEDED, Status.Code.CANCELLED, Status.Code.ABORTED -> true
            else -> false
        }
    }

    private fun markFailed(uid: String) {
        val tx = db.transactions().get(uid) ?: return
        db.transactions().update(tx.copy(status = MwebTransaction.STATUS_FAILED))
        for (coin in db.utxos().all()) {
            if (coin.spentByTxUid == uid) db.utxos().update(coin.copy(spent = false, spentByTxUid = null))
        }
    }

    // region peg-in

    /**
     * Fee breakdown of a peg-in: [litecoinFee] paid by the Litecoin inputs
     * (including the HogEx input the peg-in adds to the next block) and
     * [mwebFee] paid in litoshi on the MWEB side, both at [feeRate].
     */
    data class PegInFee(val litecoinFee: Long, val mwebFee: Long) {
        val total get() = litecoinFee + mwebFee
    }

    /**
     * Estimates the fees of moving [value] litoshi from [litecoinKit] into
     * this wallet. Throws [MwebSendError.InsufficientBalance] when the
     * Litecoin wallet cannot cover value and fees.
     */
    @Throws(MwebSendError::class)
    fun estimatePegInFee(litecoinKit: LitecoinKit, value: Long, feeRate: Int): PegInFee {
        val mwebFee = pegInMwebFee(feeRate)
        val unsigned = buildPegInTemplate(litecoinKit, value, mwebFee, feeRate)
        return PegInFee(unsigned.fee, mwebFee)
    }

    /**
     * Moves [value] litoshi from [litecoinKit] into this wallet, to the
     * receive address at [addressIndex] (default: the next unused one).
     */
    @Throws(MwebSendError::class)
    suspend fun pegIn(litecoinKit: LitecoinKit, value: Long, feeRate: Int, addressIndex: Int? = null): MwebTransactionInfo {
        val index = addressIndex ?: nextReceiveIndex()
        return pegIn(litecoinKit, addressAt(index), value, feeRate)
    }

    /**
     * Sends [value] litoshi from [litecoinKit] to any MWEB [address] of this
     * network, own or foreign. The Litecoin wallet pays the Litecoin fee and
     * keeps its change on the Litecoin side; the MWEB fee is paid out of the
     * peg-in amount, so the MWEB output carries exactly [value].
     *
     * Sequence: the Litecoin wallet builds an unsigned transaction paying
     * value plus MWEB fee to the MWEB address with its usual change; the
     * change output is set aside, because the daemon turns every non-MWEB
     * output into a peg-out, and the MWEB output is set to value; the daemon
     * replaces it with the peg-in output and attaches the MWEB extension;
     * the change output is put back; the Litecoin wallet signs; the daemon
     * broadcasts (a plain broadcast would strip the extension); both wallets
     * record the transaction.
     *
     * For a foreign address no output reaches this wallet's stream, so the
     * row confirms when the Litecoin transaction does: the kit watches it
     * through [litecoinKit] while running, and [refreshPegIns] does the same
     * on demand.
     */
    @Throws(MwebSendError::class)
    suspend fun pegIn(litecoinKit: LitecoinKit, address: String, value: Long, feeRate: Int): MwebTransactionInfo {
        val spendSecret = keychain.spendSecret ?: throw MwebSendError.WatchOnly()
        val rpc = this.rpc ?: throw MwebSendError.NotSynced()
        if (syncState !is MwebSyncState.Synced) throw MwebSendError.NotSynced()
        if (value <= 0) throw MwebSendError.DustAmount()
        val destination = MwebAddress.decode(address, hrp)
        val feeRatePerKb = feeRate.toLong() * 1000

        return dbMutex.withLock {
            val ownIndex = db.addresses().byAddress(destination.stringValue)?.index
            val mwebFee = pegInMwebFee(feeRate)

            val unsigned = buildPegInTemplate(litecoinKit, value, mwebFee, feeRate, destination.scriptPubKey)
            val template = MwebPegIn.template(unsigned.rawTransaction, unsigned.changeOutputIndex, destination.scriptPubKey, value)

            val created = try {
                rpc.create(template.rawTx, keychain.scanSecret, spendSecret, feeRatePerKb, false)
            } catch (e: Exception) {
                throw MwebSendError.BroadcastRejected("Transaction creation failed: ${e.message}", e)
            }
            val withChange = MwebPegIn.withChange(created.rawTx.toByteArray(), template.change, value + mwebFee)
            val signed = litecoinKit.signTransaction(withChange, unsigned.inputs)

            val uid = UUID.randomUUID().toString()
            val tx = MwebTransaction(
                uid = uid,
                type = MwebTransaction.TYPE_PEG_IN,
                amount = value,
                fee = unsigned.fee + mwebFee,
                timestamp = System.currentTimeMillis() / 1000,
                height = 0,
                status = MwebTransaction.STATUS_PENDING,
                toAddress = destination.stringValue,
                ownAddress = if (ownIndex != null) destination.stringValue else null,
                txHash = null,
                inputIds = "",
                outputIds = if (ownIndex != null) created.outputIdList.joinToString(",") else "",
            )
            db.transactions().insert(tx)

            val txid = broadcast(rpc, signed, uid)
            litecoinKit.registerPendingTransaction(signed)

            val sent = tx.copy(txHash = txid)
            db.transactions().update(sent)
            if (ownIndex == null) watchPegIn(litecoinKit, uid)
            sent.toInfo()
        }
    }

    /**
     * Marks pending peg-ins whose Litecoin transaction [litecoinKit] has
     * seen in a block as confirmed. Peg-ins to own addresses also confirm
     * through their MWEB output; this covers foreign destinations and
     * restarts. Returns true while any peg-in is still pending. Call it when
     * the Litecoin wallet reports new blocks.
     */
    suspend fun refreshPegIns(litecoinKit: LitecoinKit): Boolean = dbMutex.withLock {
        var pending = false
        for (tx in db.transactions().withStatus(MwebTransaction.STATUS_PENDING)) {
            if (tx.type != MwebTransaction.TYPE_PEG_IN) continue
            val hash = tx.txHash ?: continue
            val info = litecoinKit.getTransaction(hash)
            val height = info?.blockHeight
            if (height != null) {
                db.transactions().update(tx.copy(status = MwebTransaction.STATUS_CONFIRMED, height = height, timestamp = info.timestamp))
            } else {
                pending = true
            }
        }
        pending
    }

    private fun watchPegIn(litecoinKit: LitecoinKit, uid: String) {
        scope.launch {
            while (isActive && db.transactions().get(uid)?.status == MwebTransaction.STATUS_PENDING) {
                delay(PEG_IN_POLL_MS)
                try {
                    refreshPegIns(litecoinKit)
                } catch (e: Exception) {
                    Log.w(tag, "peg-in refresh failed", e)
                }
            }
        }
    }

    /** MWEB fee of a peg-in: one kernel and one MWEB output, no Litecoin-side outputs. */
    private fun pegInMwebFee(feeRate: Int): Long =
        MwebFee.estimate(listOf(TemplateOutput(MwebAddress.decode(addressAt(0), hrp).scriptPubKey, 0)), feeRate.toLong() * 1000)

    private fun buildPegInTemplate(
        litecoinKit: LitecoinKit,
        value: Long,
        mwebFee: Long,
        feeRate: Int,
        mwebScript: ByteArray = MwebAddress.decode(addressAt(0), hrp).scriptPubKey,
    ): io.horizontalsystems.bitcoincore.models.UnsignedTransaction {
        try {
            // The MWEB output is replaced by the peg-in output (OP_9 <32-byte kernel hash>, 34 bytes)
            // in the final transaction; the HogEx input of the next block is charged to the sender too.
            val pegInScriptDelta = MwebAddress.SCRIPT_SIZE - MwebPegIn.PEG_IN_SCRIPT_SIZE
            return litecoinKit.buildUnsignedTransaction(
                outputs = listOf(RawOutput(mwebScript, value + mwebFee)),
                feeRate = feeRate,
                extraVirtualBytes = MwebFee.HOGEX_INPUT_VBYTES - pegInScriptDelta,
            )
        } catch (e: SendValueErrors) {
            throw MwebSendError.InsufficientBalance()
        }
    }

    private fun nextReceiveIndex(): Int {
        val used = db.utxos().all().map { it.addressIndex }.toSet() +
                db.transactions().all().mapNotNull { tx -> tx.ownAddress?.let { db.addresses().byAddress(it)?.index } }
        var index = 1
        while (index in used) index++
        return index
    }

    // endregion

    private class CoinSelection(val coins: List<MwebUtxo>, val outputs: List<TemplateOutput>, val fee: Long, val change: Long)

    private fun selectCoins(script: ByteArray, value: Long, feeRatePerKb: Long): CoinSelection {
        if (value <= 0) throw MwebSendError.DustAmount()
        val changeScript = MwebAddress.decode(addressAt(0), hrp).scriptPubKey
        val candidates = db.utxos().unspent()
            .filter { isConfirmed(it, _lastBlockHeight.value) }
            .sortedByDescending { it.value }
        val recipient = TemplateOutput(script, value)
        val selected = ArrayList<MwebUtxo>()
        var sum = 0L
        for (coin in candidates) {
            selected.add(coin)
            sum += coin.value
            val feeNoChange = MwebFee.estimate(listOf(recipient), feeRatePerKb)
            if (sum < value + feeNoChange) continue
            val feeWithChange = MwebFee.estimate(listOf(recipient, TemplateOutput(changeScript, 0)), feeRatePerKb)
            val change = sum - value - feeWithChange
            return if (change > MWEB_CHANGE_THRESHOLD) {
                CoinSelection(selected, listOf(recipient, TemplateOutput(changeScript, change)), feeWithChange, change)
            } else {
                // Too little for a change output: the surplus becomes fee.
                CoinSelection(selected, listOf(recipient), sum - value, 0)
            }
        }
        throw MwebSendError.InsufficientBalance()
    }

    /** Output script for [address]: 66 key bytes for MWEB, a Litecoin locking script otherwise. */
    @Throws(MwebAddressFormatException::class)
    private fun destinationScript(address: String): ByteArray {
        if (address.lowercase().startsWith("${hrp}1")) {
            return MwebAddress.decode(address, hrp).scriptPubKey
        }
        val litecoinAddress: Address = try {
            litecoinAddressConverter.convert(address)
        } catch (e: Exception) {
            throw MwebAddressFormatException("Not an MWEB or Litecoin address")
        }
        return litecoinAddress.lockingScript
    }

    // endregion

    // region sync

    private suspend fun runSync() {
        var backoff = 1_000L
        while (scope.isActive) {
            try {
                _syncState.value = MwebSyncState.Syncing(0.0, MwebSyncState.Stage.Starting)
                val port = withContext(Dispatchers.IO) { MwebDaemon.start(context, networkType, proxyUrl) }
                val rpc = MwebRpc(port).also { this.rpc = it }
                ensureAddresses(ADDRESS_GAP)
                backoff = 1_000L

                val statusJob = scope.launch { pollStatus(rpc) }
                try {
                    consumeUtxos(rpc)
                } finally {
                    statusJob.cancel()
                }
            } catch (e: Exception) {
                if (!scope.isActive) return
                Log.w(tag, "sync failed, retrying in ${backoff}ms", e)
                _syncState.value = MwebSyncState.NotSynced(e)
                rpc?.shutdown()
                rpc = null
                delay(backoff)
                backoff = min(backoff * 2, 60_000L)
            }
        }
    }

    private suspend fun pollStatus(rpc: MwebRpc) {
        var lastHeight = -1
        while (true) {
            try {
                val status = rpc.status()
                val headerHeight = status.blockHeaderHeight
                val now = System.currentTimeMillis() / 1000
                val blocksBehind = max(0L, (now - status.blockTime) / BLOCK_INTERVAL_SECONDS)
                val headersSynced = blocksBehind <= TIP_TOLERANCE_BLOCKS
                val mwebSynced = status.mwebUtxosHeight > 0 && status.mwebUtxosHeight >= headerHeight
                _lastBlockHeight.value = headerHeight

                if (headersSynced && mwebSynced) {
                    if (utxoStreamStarted) _syncState.value = MwebSyncState.Synced
                    if (headerHeight != lastHeight) {
                        lastHeight = headerHeight
                        reconcile(rpc)
                        if (utxoStreamStarted) db.syncInfo().set(MwebSyncInfo(syncedHeight = headerHeight))
                    }
                } else if (headersSynced) {
                    _syncState.value = MwebSyncState.Syncing(0.99, MwebSyncState.Stage.MwebData)
                } else {
                    val total = headerHeight + blocksBehind
                    val progress = if (total > 0) headerHeight.toDouble() / total else 0.0
                    _syncState.value = MwebSyncState.Syncing(min(progress, 0.98), MwebSyncState.Stage.BlockHeaders)
                }
            } catch (e: Exception) {
                Log.w(tag, "status failed", e)
            }
            delay(STATUS_INTERVAL_MS)
        }
    }

    @Volatile
    private var utxoStreamStarted = false

    private suspend fun consumeUtxos(rpc: MwebRpc) {
        val synced = db.syncInfo().get()?.syncedHeight ?: 0
        val fromHeight = max(0, synced - RESCAN_MARGIN_BLOCKS)
        utxoStreamStarted = true
        rpc.utxos(keychain.scanSecret, fromHeight).collect { utxo ->
            // The already confirmed outputs arrive first in one burst; the stream then stays open
            // for live updates. The daemon sends an empty marker before the first live update.
            if (utxo.outputId.isEmpty()) {
                reconcile(rpc)
                return@collect
            }
            dbMutex.withLock { handleUtxo(utxo) }
        }
        throw IllegalStateException("utxo stream ended")
    }

    private fun handleUtxo(utxo: Utxo) {
        val existing = db.utxos().get(utxo.outputId)
        if (existing != null) {
            if (utxo.height > 0 && existing.height != utxo.height) {
                db.utxos().update(existing.copy(height = utxo.height, blockTime = utxo.blockTime.toLong()))
                confirmOwnTransaction(existing.createdByTxUid, utxo.height, utxo.blockTime.toLong().takeIf { it > 0 })
            }
            return
        }

        val index = addressIndexOf(utxo.address)
        if (index == null) {
            Log.w(tag, "output ${utxo.outputId} on unknown address, ignored")
            return
        }

        val ownTx = db.transactions().all().firstOrNull { tx -> utxo.outputId in tx.outputIds.split(',') }
        // The daemon reports no block time for outputs mined below the header
        // checkpoint, where it holds no headers. Such a row keeps timestamp 0,
        // "unknown", rather than the time of the restore; only an unconfirmed
        // output is dated by the clock.
        val timestamp = when {
            utxo.blockTime > 0 -> utxo.blockTime.toLong()
            utxo.height > 0 -> 0L
            else -> System.currentTimeMillis() / 1000
        }
        db.utxos().insert(
            MwebUtxo(
                outputId = utxo.outputId,
                value = utxo.value,
                address = utxo.address,
                addressIndex = index,
                height = utxo.height,
                blockTime = timestamp,
                createdByTxUid = ownTx?.uid,
            )
        )

        if (ownTx != null) {
            if (utxo.height > 0) confirmOwnTransaction(ownTx.uid, utxo.height, timestamp.takeIf { it > 0 })
            return
        }

        db.transactions().insert(
            MwebTransaction(
                uid = "in-${utxo.outputId}",
                type = MwebTransaction.TYPE_INCOMING,
                amount = utxo.value,
                fee = null,
                timestamp = timestamp,
                height = utxo.height,
                status = if (utxo.height > 0) MwebTransaction.STATUS_CONFIRMED else MwebTransaction.STATUS_PENDING,
                toAddress = null,
                ownAddress = utxo.address,
                txHash = null,
                inputIds = "",
                outputIds = utxo.outputId,
            )
        )
    }

    private fun confirmOwnTransaction(uid: String?, height: Int, timestamp: Long?) {
        val tx = db.transactions().get(uid ?: return) ?: return
        if (tx.status == MwebTransaction.STATUS_CONFIRMED) return
        db.transactions().update(tx.copy(status = MwebTransaction.STATUS_CONFIRMED, height = height, timestamp = timestamp ?: tx.timestamp))
    }

    /**
     * Asks the daemon which of our outputs left the unspent set: inputs of
     * pending sends confirm, and outputs spent by another wallet on the
     * same seed are recorded as such.
     */
    private suspend fun reconcile(rpc: MwebRpc) {
        val all = dbMutex.withLock { db.utxos().all() }
        val confirmed = all.filter { it.height > 0 }
        val candidates = confirmed.filter { !it.spent || (it.spentByTxUid != null && pendingUid(it.spentByTxUid)) }
        if (candidates.isEmpty()) return
        val gone = rpc.spent(candidates.map { it.outputId }).toSet()
        if (gone.isEmpty()) return

        dbMutex.withLock {
            val height = _lastBlockHeight.value
            val now = System.currentTimeMillis() / 1000
            val elsewhere = ArrayList<MwebUtxo>()
            for (utxo in candidates) {
                if (utxo.outputId !in gone) continue
                if (utxo.spentByTxUid != null) {
                    db.transactions().get(utxo.spentByTxUid)?.let { tx ->
                        // A peg-out or a send without change has no own output to watch; its inputs leaving the set means it confirmed.
                        if (tx.status == MwebTransaction.STATUS_PENDING && tx.outputIds.isEmpty()) {
                            db.transactions().update(tx.copy(status = MwebTransaction.STATUS_CONFIRMED, height = height))
                        }
                    }
                } else {
                    db.utxos().update(utxo.copy(spent = true))
                    elsewhere.add(utxo)
                }
            }
            if (elsewhere.isNotEmpty()) {
                val uid = "spent-${elsewhere.first().outputId}"
                db.transactions().insert(
                    MwebTransaction(
                        uid = uid,
                        type = MwebTransaction.TYPE_SPENT_ELSEWHERE,
                        amount = elsewhere.sumOf { it.value },
                        fee = null,
                        timestamp = now,
                        height = height,
                        status = MwebTransaction.STATUS_CONFIRMED,
                        toAddress = null,
                        ownAddress = null,
                        txHash = null,
                        inputIds = elsewhere.joinToString(",") { it.outputId },
                        outputIds = "",
                    )
                )
                for (utxo in elsewhere) db.utxos().update(utxo.copy(spent = true, spentByTxUid = uid))
            }
        }
    }

    private fun pendingUid(uid: String) = db.transactions().get(uid)?.status == MwebTransaction.STATUS_PENDING

    // endregion

    internal fun MwebTransaction.toInfo() = MwebTransactionInfo(
        uid = uid,
        type = when (type) {
            MwebTransaction.TYPE_INCOMING -> MwebTransactionType.Incoming
            MwebTransaction.TYPE_OUTGOING -> MwebTransactionType.Outgoing
            MwebTransaction.TYPE_PEG_IN -> MwebTransactionType.PegIn
            MwebTransaction.TYPE_PEG_OUT -> MwebTransactionType.PegOut
            else -> MwebTransactionType.SpentElsewhere
        },
        amount = amount,
        fee = fee,
        timestamp = timestamp.takeIf { it > 0 },
        blockHeight = height.takeIf { it > 0 },
        status = when (status) {
            MwebTransaction.STATUS_CONFIRMED -> MwebTransactionStatus.Confirmed
            MwebTransaction.STATUS_FAILED -> MwebTransactionStatus.Failed
            else -> MwebTransactionStatus.Pending
        },
        toAddress = toAddress,
        ownAddress = ownAddress,
        txHash = txHash,
        inputIds = inputIds.split(',').filter { it.isNotEmpty() },
        outputIds = outputIds.split(',').filter { it.isNotEmpty() },
    )

    companion object {
        /** Receive addresses derived ahead of the highest index seen. */
        const val ADDRESS_GAP = 20

        /** Highest address index the kit recognises outputs on. */
        const val ADDRESS_POOL_LIMIT = 1000

        /** Blocks re-streamed below the last synced height on start, covering reorgs. */
        const val RESCAN_MARGIN_BLOCKS = 10

        /** Below this a would-be change output is left to the fee instead. */
        const val MWEB_CHANGE_THRESHOLD = 1000L

        /** Dust limit of a Litecoin output, applied to peg-outs. */
        const val LITECOIN_DUST = 546L

        private const val PEG_IN_POLL_MS = 30_000L

        private const val BLOCK_INTERVAL_SECONDS = 150L
        private const val TIP_TOLERANCE_BLOCKS = 24L
        private const val STATUS_INTERVAL_MS = 5_000L

        private fun databaseName(networkType: LitecoinKit.NetworkType, walletId: String) =
            "LitecoinMweb-${networkType.name}-$walletId"

        /** Deletes the wallet's outputs, addresses and history. Shared chain data stays. */
        fun clear(context: Context, networkType: LitecoinKit.NetworkType, walletId: String) {
            try {
                SQLiteDatabase.deleteDatabase(context.getDatabasePath(databaseName(networkType, walletId)))
            } catch (e: Exception) {
                // nothing to delete
            }
        }

        /** The first receive address (index 1) of the wallet with [seed]. */
        fun firstAddress(seed: ByteArray, networkType: LitecoinKit.NetworkType = LitecoinKit.NetworkType.MainNet): String =
            address(MwebKeychain.fromSeed(seed), 1, networkType)

        fun address(keychain: MwebKeychain, index: Int, networkType: LitecoinKit.NetworkType): String =
            MwebNative.addresses(
                keychain.scanSecret.toHexString(),
                keychain.spendPubKey.toHexString(),
                MwebDaemon.chainName(networkType),
                index,
                index + 1
            ).trim()
    }
}
