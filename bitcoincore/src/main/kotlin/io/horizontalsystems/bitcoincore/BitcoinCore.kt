package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApi
import io.horizontalsystems.bitcoincore.blocks.IPeerSyncListener
import io.horizontalsystems.bitcoincore.core.AccountWallet
import io.horizontalsystems.bitcoincore.core.DataProvider
import io.horizontalsystems.bitcoincore.core.IConnectionManager
import io.horizontalsystems.bitcoincore.core.IInitialDownload
import io.horizontalsystems.bitcoincore.core.IKitStateListener
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.core.IPublicKeyManager
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.core.Wallet
import io.horizontalsystems.bitcoincore.core.WatchAccountWallet
import io.horizontalsystems.bitcoincore.core.description
import io.horizontalsystems.bitcoincore.core.scriptType
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.managers.IRestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.IUnspentOutputSelector
import io.horizontalsystems.bitcoincore.managers.RestoreKeyConverterChain
import io.horizontalsystems.bitcoincore.managers.SyncManager
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorChain
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.models.BitcoinSendInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.UsedAddress
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageParser
import io.horizontalsystems.bitcoincore.network.messages.NetworkMessageSerializer
import io.horizontalsystems.bitcoincore.network.peer.IInventoryItemsHandler
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.InventoryItemsHandlerChain
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import io.horizontalsystems.bitcoincore.network.peer.PeerTaskHandlerChain
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransaction
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransactionBuilder
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransactionInfo
import io.horizontalsystems.bitcoincore.rbf.ReplacementType
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.bitcoincore.transactions.TransactionCreator
import io.horizontalsystems.bitcoincore.transactions.TransactionFeeCalculator
import io.horizontalsystems.bitcoincore.transactions.TransactionSyncer
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.DirectExecutor
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.hdwalletkit.HDWalletAccount
import io.horizontalsystems.hdwalletkit.HDWalletAccountWatch
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.roundToInt

class BitcoinCore(
    private val storage: IStorage,
    private val dataProvider: DataProvider,
    private val publicKeyManager: IPublicKeyManager,
    private val addressConverter: AddressConverterChain,
    private val restoreKeyConverterChain: RestoreKeyConverterChain,
    private val transactionCreator: TransactionCreator?,
    private val transactionFeeCalculator: TransactionFeeCalculator?,
    private val replacementTransactionBuilder: ReplacementTransactionBuilder?,
    private val paymentAddressParser: PaymentAddressParser,
    private val syncManager: SyncManager,
    private val purpose: Purpose,
    private var peerManager: PeerManager,
    private val dustCalculator: DustCalculator?,
    private val pluginManager: PluginManager,
    private val connectionManager: IConnectionManager
) : IKitStateListener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit
        fun onTransactionsDelete(hashes: List<String>) = Unit
        fun onBalanceUpdate(balance: BalanceInfo) = Unit
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo) = Unit
        fun onKitStateUpdate(state: KitState) = Unit
    }

    // START: Extending
    lateinit var peerGroup: PeerGroup
    lateinit var transactionSyncer: TransactionSyncer
    lateinit var networkMessageParser: NetworkMessageParser
    lateinit var networkMessageSerializer: NetworkMessageSerializer
    lateinit var initialDownload: IInitialDownload
    lateinit var unspentOutputSelector: UnspentOutputSelectorChain
    lateinit var watchedTransactionManager: WatchedTransactionManager

    val inventoryItemsHandlerChain = InventoryItemsHandlerChain()
    val peerTaskHandlerChain = PeerTaskHandlerChain()

    fun addPeerSyncListener(peerSyncListener: IPeerSyncListener): BitcoinCore {
        initialDownload.addPeerSyncListener(peerSyncListener)
        return this
    }

    fun addRestoreKeyConverter(keyConverter: IRestoreKeyConverter) {
        restoreKeyConverterChain.add(keyConverter)
    }

    fun addMessageParser(messageParser: IMessageParser): BitcoinCore {
        networkMessageParser.add(messageParser)
        return this
    }

    fun addMessageSerializer(messageSerializer: IMessageSerializer): BitcoinCore {
        networkMessageSerializer.add(messageSerializer)
        return this
    }

    fun addInventoryItemsHandler(handler: IInventoryItemsHandler) {
        inventoryItemsHandlerChain.addHandler(handler)
    }

    fun addPeerTaskHandler(handler: IPeerTaskHandler) {
        peerTaskHandlerChain.addHandler(handler)
    }

    fun addPeerGroupListener(listener: PeerGroup.Listener) {
        peerGroup.addPeerGroupListener(listener)
    }

    fun prependUnspentOutputSelector(selector: IUnspentOutputSelector) {
        unspentOutputSelector.prependSelector(selector)
    }

    fun prependAddressConverter(converter: IAddressConverter) {
        addressConverter.prependConverter(converter)
    }

    // END: Extending

    var listenerExecutor: Executor = DirectExecutor()

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val lastBlockInfo get() = dataProvider.lastBlockInfo
    val syncState get() = syncManager.syncState

    var listener: Listener? = null

    val watchAccount: Boolean
        get() = transactionCreator == null

    fun getUnspentOutputs(filters: UtxoFilters): List<UnspentOutputInfo> {
        return unspentOutputSelector.getAllSpendable(filters).map {
            UnspentOutputInfo.fromUnspentOutput(it)
        }
    }

    //
    // API methods
    //
    fun start() {
        syncManager.start()
    }

    fun stop() {
        dataProvider.clear()
        syncManager.stop()
    }

    fun refresh() {
        start()
    }

    fun onEnterForeground() {
        connectionManager.onEnterForeground()
    }

    fun onEnterBackground() {
        connectionManager.onEnterBackground()
    }

    fun transactions(fromUid: String? = null, type: TransactionFilterType? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return dataProvider.transactions(fromUid, type, limit)
    }

    fun sendInfo(
        value: Long,
        address: String? = null,
        memo: String?,
        senderPay: Boolean = true,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>,
        dustThreshold: Int?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): BitcoinSendInfo {
        val outputs = unspentOutputs?.mapNotNull {
            unspentOutputSelector.getAllSpendable(filters).firstOrNull { unspentOutput ->
                unspentOutput.transaction.hash.contentEquals(it.transactionHash) && unspentOutput.output.index == it.outputIndex
            }
        }
        return transactionFeeCalculator?.sendInfo(
            value = value,
            feeRate = feeRate,
            senderPay = senderPay,
            toAddress = address,
            memo = memo,
            unspentOutputs = outputs,
            pluginData = pluginData,
            dustThreshold = dustThreshold,
            changeToFirstInput = changeToFirstInput,
            filters = filters,
        ) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        address: String,
        memo: String?,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>,
        rbfEnabled: Boolean,
        dustThreshold: Int?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): FullTransaction {
        val outputs = unspentOutputs?.mapNotNull {
            unspentOutputSelector.getAllSpendable(filters).firstOrNull { unspentOutput ->
                unspentOutput.transaction.hash.contentEquals(it.transactionHash) && unspentOutput.output.index == it.outputIndex
            }
        }
        return transactionCreator?.create(
            toAddress = address,
            memo = memo,
            value = value,
            feeRate = feeRate,
            senderPay = senderPay,
            sortType = sortType,
            unspentOutputs = outputs,
            pluginData = pluginData,
            rbfEnabled = rbfEnabled,
            dustThreshold = dustThreshold,
            changeToFirstInput = changeToFirstInput,
            filters = filters,
        ) ?: throw CoreError.ReadOnlyCore
    }

    fun send(
        hash: ByteArray,
        memo: String?,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        unspentOutputs: List<UnspentOutputInfo>?,
        rbfEnabled: Boolean,
        dustThreshold: Int?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): FullTransaction {
        val address = addressConverter.convert(hash, scriptType)
        val outputs = unspentOutputs?.mapNotNull {
            unspentOutputSelector.getAllSpendable(filters).firstOrNull { unspentOutput ->
                unspentOutput.transaction.hash.contentEquals(it.transactionHash) && unspentOutput.output.index == it.outputIndex
            }
        }
        return transactionCreator?.create(
            toAddress = address.stringValue,
            memo = memo,
            value = value,
            feeRate = feeRate,
            senderPay = senderPay,
            sortType = sortType,
            unspentOutputs = outputs,
            pluginData = mapOf(),
            rbfEnabled = rbfEnabled,
            dustThreshold = dustThreshold,
            changeToFirstInput = changeToFirstInput,
            filters = filters,
        ) ?: throw CoreError.ReadOnlyCore
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, memo: String?, feeRate: Int, sortType: TransactionDataSortType, rbfEnabled: Boolean): FullTransaction {
        return transactionCreator?.create(unspentOutput, address, memo, feeRate, sortType, rbfEnabled) ?: throw CoreError.ReadOnlyCore
    }

    fun receiveAddress(): String {
        return addressConverter.convert(publicKeyManager.receivePublicKey(), purpose.scriptType).stringValue
    }

    fun address(publicKey: PublicKey): Address {
        return addressConverter.convert(publicKey, purpose.scriptType)
    }

    fun usedAddresses(change: Boolean): List<UsedAddress> {
        return publicKeyManager.usedExternalPublicKeys(change).map {
            UsedAddress(
                index = it.index,
                address = addressConverter.convert(it, purpose.scriptType).stringValue
            )
        }.sortedBy { it.index }
    }

    fun receivePublicKey(): PublicKey {
        return publicKeyManager.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return publicKeyManager.changePublicKey()
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        return publicKeyManager.getPublicKeyByPath(path)
    }

    fun validateAddress(address: String, pluginData: Map<Byte, IPluginData> = mapOf()) {
        pluginManager.validateAddress(addressConverter.convert(address), pluginData)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return paymentAddressParser.parse(paymentAddress)
    }

    fun showDebugInfo() {
        publicKeyManager.fillGap()
        storage.getPublicKeys().forEach { pubKey ->
            try {
//                    val scriptType = if (network is MainNetBitcoinCash || network is TestNetBitcoinCash)
//                        ScriptType.P2PKH else
//                        ScriptType.P2WPKH

                val legacy = addressConverter.convert(pubKey.publicKeyHash, ScriptType.P2PKH).stringValue
//                    val wpkh = addressConverter.convert(pubKey.scriptHashP2WPKH, ScriptType.P2SH).string
//                    val bechAddress = try {
//                        addressConverter.convert(OpCodes.push(0) + OpCodes.push(pubKey.publicKeyHash), scriptType).string
//                    } catch (e: Exception) {
//                        ""
//                    }
                println("${pubKey.index} --- extrnl: ${pubKey.external} --- hash: ${pubKey.publicKeyHash.toHexString()} ---- legacy: $legacy")
//                    println("legacy: $legacy --- bech32: $bechAddress --- SH(WPKH): $wpkh")
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Synced Until"] = lastBlockInfo?.timestamp?.let { Date(it * 1000) } ?: "N/A"
        statusInfo["Syncing Peer"] = initialDownload.syncPeer?.host ?: "N/A"
        statusInfo["Derivation"] = purpose.description
        statusInfo["Sync State"] = syncState.toString()
        statusInfo["Last Block Height"] = lastBlockInfo?.height ?: "N/A"

        val peers = LinkedHashMap<String, Any>()
        peerManager.connected().forEachIndexed { index, peer ->

            val peerStatus = LinkedHashMap<String, Any>()
            peerStatus["Status"] = if (peer.synced) "Synced" else "Not Synced"
            peerStatus["Host"] = peer.host
            peerStatus["Best Block"] = peer.announcedLastBlockHeight
            peerStatus["User Agent"] = peer.subVersion

            peer.tasks.let { peerTasks ->
                if (peerTasks.isEmpty()) {
                    peerStatus["tasks"] = "no tasks"
                } else {
                    val tasks = LinkedHashMap<String, Any>()
                    peerTasks.forEach { task ->
                        tasks[task.javaClass.simpleName] = "[${task.state}]"
                    }
                    peerStatus["tasks"] = tasks
                }
            }

            peers["Peer ${index + 1}"] = peerStatus
        }

        statusInfo.putAll(peers)

        return statusInfo
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        listenerExecutor.execute {
            listener?.onTransactionsUpdate(inserted, updated)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listenerExecutor.execute {
            listener?.onTransactionsDelete(hashes)
        }
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        listenerExecutor.execute {
            listener?.onBalanceUpdate(balance)
        }
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listenerExecutor.execute {
            listener?.onLastBlockInfoUpdate(blockInfo)
        }
    }

    //
    // IKitStateManagerListener implementations
    //
    override fun onKitStateUpdate(state: KitState) {
        listenerExecutor.execute {
            listener?.onKitStateUpdate(state)
        }
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        watchedTransactionManager.add(filter, listener)
    }

    fun maximumSpendableValue(
        address: String?,
        memo: String?,
        feeRate: Int,
        unspentOutputInfos: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>,
        dustThreshold: Int?,
        changeToFirstInput: Boolean,
        filters: UtxoFilters
    ): Long {
        if (transactionFeeCalculator == null) throw CoreError.ReadOnlyCore

        val outputs = unspentOutputInfos?.let { getOutputsFromInfos(it, filters) }

        val spendableBalance = when {
            outputs == null && filters.isEmpty() -> {
                balance.spendable
            }
            outputs != null -> {
                outputs.sumOf { it.output.value }
            }
            else -> {
                unspentOutputSelector.getAllSpendable(filters).sumOf { it.output.value }
            }
        }

        val sendAllFee = transactionFeeCalculator.sendInfo(
            value = spendableBalance,
            feeRate = feeRate,
            senderPay = false,
            toAddress = address,
            memo = memo,
            unspentOutputs = outputs,
            pluginData = pluginData,
            dustThreshold = dustThreshold,
            changeToFirstInput = changeToFirstInput,
            filters = filters,
        ).fee

        return max(0L, spendableBalance - sendAllFee)
    }

    private fun getOutputsFromInfos(
        unspentOutputInfos: List<UnspentOutputInfo>,
        filters: UtxoFilters,
    ): List<UnspentOutput> {
        val allSpendable = unspentOutputSelector.getAllSpendable(filters)

        return unspentOutputInfos.mapNotNull { unspentOutputInfo ->
            allSpendable.firstOrNull { unspentOutput ->
                unspentOutput.transaction.hash.contentEquals(unspentOutputInfo.transactionHash) &&
                    unspentOutput.output.index == unspentOutputInfo.outputIndex
            }
        }
    }

    fun minimumSpendableValue(address: String?, dustThreshold: Int?): Int {
        // by default script type is P2PKH, since it is most used
        val scriptType = when {
            address != null -> addressConverter.convert(address).scriptType
            else -> ScriptType.P2PKH
        }

        return dustCalculator?.dust(scriptType, dustThreshold) ?: throw CoreError.ReadOnlyCore
    }

    fun getRawTransaction(transactionHash: String): String? {
        return dataProvider.getRawTransaction(transactionHash)
    }

    fun getTransaction(hash: String): TransactionInfo? {
        return dataProvider.getTransaction(hash)
    }

    fun replacementTransaction(
        transactionHash: String,
        minFee: Long,
        type: ReplacementType,
        dustThreshold: Int?,
        filters: UtxoFilters
    ): ReplacementTransaction {
        val replacementTransactionBuilder = this.replacementTransactionBuilder ?: throw CoreError.ReadOnlyCore

        val (mutableTransaction, fullInfo, descendantTransactionHashes) =
            replacementTransactionBuilder.replacementTransaction(
                transactionHash,
                minFee,
                type,
                dustThreshold,
                filters
            )
        val info = dataProvider.transactionInfo(fullInfo)
        return ReplacementTransaction(mutableTransaction, info, descendantTransactionHashes)
    }

    fun send(replacementTransaction: ReplacementTransaction): FullTransaction {
        val transactionCreator = this.transactionCreator ?: throw CoreError.ReadOnlyCore

        return transactionCreator.create(replacementTransaction.mutableTransaction)
    }

    fun replacementTransactionInfo(
        transactionHash: String,
        type: ReplacementType,
        dustThreshold: Int?,
        filters: UtxoFilters
    ): ReplacementTransactionInfo? {
        return replacementTransactionBuilder?.replacementInfo(transactionHash, type, dustThreshold, filters)
    }

    sealed class KitState {
        object Synced : KitState()
        class NotSynced(val exception: Throwable) : KitState()
        class Syncing(val progress: Double) : KitState()
        class ApiSyncing(val transactions: Int) : KitState()

        override fun equals(other: Any?) = when {
            this is Synced && other is Synced -> true
            this is NotSynced && other is NotSynced -> exception == other.exception
            this is Syncing && other is Syncing -> this.progress == other.progress
            this is ApiSyncing && other is ApiSyncing -> this.transactions == other.transactions
            else -> false
        }

        override fun toString() = when (this) {
            is Synced -> "Synced"
            is NotSynced -> "NotSynced-${this.exception.javaClass.simpleName}"
            is Syncing -> "Syncing-${(this.progress * 100).roundToInt() / 100.0}"
            is ApiSyncing -> "ApiSyncing-$transactions"
        }

        override fun hashCode(): Int {
            var result = javaClass.hashCode()
            if (this is Syncing) {
                result = 31 * result + progress.hashCode()
            }
            if (this is NotSynced) {
                result = 31 * result + exception.hashCode()
            }
            if (this is ApiSyncing) {
                result = 31 * result + transactions.hashCode()
            }
            return result
        }
    }

    sealed class SyncMode {
        class Full : SyncMode()
        class Api : SyncMode()
        class Blockchair : SyncMode()
    }

    sealed class StateError : Exception() {
        class NotStarted : StateError()
        class NoInternet : StateError()
    }

    sealed class CoreError : Exception() {
        object ReadOnlyCore : CoreError()
    }

    sealed class SendType {
        object P2P: SendType()
        class API(val blockchairApi: BlockchairApi): SendType()
    }

    companion object {
        fun firstAddress(seed: ByteArray, purpose: Purpose, network: Network, addressConverter: AddressConverterChain) : Address {
            val wallet = Wallet(HDWallet(seed, network.coinType, purpose), 20)
            val publicKey = wallet.publicKey(0, 0, true)

            return addressConverter.convert(publicKey, purpose.scriptType)
        }

        fun firstAddress(
            extendedKey: HDExtendedKey,
            purpose: Purpose,
            network: Network,
            addressConverter: AddressConverterChain
        ): Address {
            val publicKey = if (!extendedKey.isPublic) {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master -> {
                        val wallet = Wallet(HDWallet(extendedKey.key, network.coinType, purpose), 0)
                        wallet.publicKey(0, 0, true)
                    }

                    HDExtendedKey.DerivedType.Account -> {
                        val wallet = AccountWallet(HDWalletAccount(extendedKey.key), 0)
                        wallet.publicKey(0,true)
                    }

                    HDExtendedKey.DerivedType.Bip32 -> {
                        throw IllegalStateException("Custom Bip32 Extended Keys are not supported")
                    }
                }
            } else {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Account -> {
                        val wallet = WatchAccountWallet(HDWalletAccountWatch(extendedKey.key), 0)
                        wallet.publicKey(0,true)
                    }

                    HDExtendedKey.DerivedType.Bip32, HDExtendedKey.DerivedType.Master -> {
                        throw IllegalStateException("Only Account Extended Public Keys are supported")
                    }
                }
            }

            return addressConverter.convert(publicKey, purpose.scriptType)
        }
    }

}
