package io.horizontalsystems.litecoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.apisync.BCoinApi
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApi
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairTransactionProvider
import io.horizontalsystems.bitcoincore.blocks.validators.BitsValidator
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyTestNetDifficultyValidator
import io.horizontalsystems.bitcoincore.core.purpose
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.managers.Bip44RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip49RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip84RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip86RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.WatchAddressPublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.litecoinkit.mweb.MwebAddressConverter
import io.horizontalsystems.litecoinkit.mweb.MwebBech32
import io.horizontalsystems.litecoinkit.mweb.MwebKeychain
import io.horizontalsystems.litecoinkit.mweb.MwebManager
import io.horizontalsystems.litecoinkit.mweb.MwebScanner
import io.horizontalsystems.litecoinkit.mweb.MwebSigner
import io.horizontalsystems.litecoinkit.mweb.MwebTransactionBuilder
import io.horizontalsystems.litecoinkit.mweb.network.messages.GetMwebUtxosMessageParser
import io.horizontalsystems.litecoinkit.mweb.network.messages.GetMwebUtxosMessageSerializer
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebTransactionMessageParser
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebTransactionMessageSerializer
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebUtxosMessageParser
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebUtxosMessageSerializer
import io.horizontalsystems.litecoinkit.mweb.storage.MwebDatabase
import io.horizontalsystems.litecoinkit.mweb.storage.MwebStorage
import io.horizontalsystems.litecoinkit.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.litecoinkit.validators.ProofOfWorkValidator

class LitecoinKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    /** Non-null when the kit was created from a master HD key and MWEB keys could be derived. */
    var mwebKeychain: MwebKeychain? = null
        private set

    /** Non-null once the kit is fully initialized; manages MWEB UTXO syncing. */
    var mwebManager: MwebManager? = null
        private set

    private var mwebStorage: MwebStorage? = null
    private var mwebTransactionBuilder: MwebTransactionBuilder? = null

    /** Total MWEB balance in satoshis (unspent owned outputs only). Returns 0 if MWEB not initialized. */
    val mwebBalance: Long
        get() = mwebStorage?.totalBalance() ?: 0L

    /** Number of unspent owned MWEB outputs. Returns 0 if MWEB not initialized. */
    val mwebUnspentOutputCount: Int
        get() = mwebStorage?.unspentOutputCount() ?: 0

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    /**
     * Returns the MWEB stealth address for this wallet (ltcmweb1... on mainnet, tmweb1... on testnet).
     * Returns null if the kit was created from an account key or a watch address.
     */
    fun mwebAddress(): String? = mwebKeychain?.let { keychain ->
        val hrp = if (network is MainNetLitecoin) "ltcmweb" else "tmweb"
        MwebBech32.encode(hrp, keychain.scanPubKey, keychain.spendPubKey)
    }

    /**
     * Sends MWEB funds to a canonical Litecoin address via a peg-out.
     *
     * **Limitation**: Change is not supported — [amount] + [fee] must exactly equal
     * the sum of selected UTXOs. Use [estimatePegOutFee] to compute the fee first,
     * then choose an amount accordingly (or send all available MWEB balance).
     *
     * @param toAddress canonical Litecoin destination (ltc1..., L..., M...)
     * @param amount    amount to send in satoshis (excluding fee)
     * @param fee       fee in satoshis (use [estimatePegOutFee] to estimate)
     * @throws IllegalStateException    if MWEB spending is unavailable (watch-only wallet or no master key)
     * @throws IllegalArgumentException if MWEB balance is insufficient
     * @throws IllegalStateException    if no MWEB peer is connected yet
     */
    fun pegOut(toAddress: String, amount: Long, fee: Long) {
        val builder = mwebTransactionBuilder
            ?: throw IllegalStateException("MWEB peg-out requires a master HD key")
        val manager = mwebManager
            ?: throw IllegalStateException("MwebManager not initialized")

        val address = parseAddress(toAddress, network)
        val pegOutScript = buildScriptPubKey(address)

        val mwebTx = builder.buildPegOut(pegOutScript = pegOutScript, sendAmount = amount, fee = fee)
        val mwebTxBytes = mwebTx.serialize()

        // Optimistically mark spent inputs; will be reconciled on next MWEB sync
        val spentIds = mwebTx.inputs.map { input -> input.outputId.joinToString("") { "%02x".format(it) } }
        mwebStorage?.markOutputsAsSpent(spentIds)

        manager.broadcastMwebTransaction(mwebTxBytes)
    }

    /**
     * Estimates the fee for a peg-out of [amount] satoshis at [feeRate] sat/byte.
     * Returns 0 if MWEB is not initialized.
     */
    fun estimatePegOutFee(amount: Long, feeRate: Int): Long =
        mwebTransactionBuilder?.estimateFee(amount, feeRate) ?: 0L

    private fun buildScriptPubKey(address: Address): ByteArray = address.lockingScript

    constructor(
        context: Context,
        words: List<String>,
        passphrase: String,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold,
        purpose: Purpose = Purpose.BIP44
    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, peerSize, syncMode, confirmationsThreshold, purpose)

    constructor(
        context: Context,
        seed: ByteArray,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold,
        purpose: Purpose = Purpose.BIP44
    ) : this(context, HDExtendedKey(seed, purpose), purpose, walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context
     * @param extendedKey HDExtendedKey that contains HDKey and version
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     */
    constructor(
        context: Context,
        extendedKey: HDExtendedKey,
        purpose: Purpose,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) {
        network = network(networkType)
        mwebKeychain = MwebKeychain.tryCreate(extendedKey, networkType)

        bitcoinCore = bitcoinCore(
            context = context,
            extendedKey = extendedKey,
            watchAddressPublicKey = null,
            networkType = networkType,
            walletId = walletId,
            syncMode = syncMode,
            purpose = purpose,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )

        val mwebDb = MwebDatabase.getInstance(context, getMwebDatabaseName(networkType, walletId))
        val storage = MwebStorage(mwebDb)
        this.mwebStorage = storage
        val mwebScanner = mwebKeychain?.let { MwebScanner(it) }
        val mwebSigner = mwebKeychain?.let { MwebSigner(it) }
        if (mwebSigner != null) {
            mwebTransactionBuilder = MwebTransactionBuilder(storage, mwebSigner)
        }
        val manager = MwebManager(
            mwebStorage = storage,
            scanner = mwebScanner,
            lastBlockHashProvider = { bitcoinCore.lastBlockInfo?.headerHash?.toReversedByteArray() }
        )
        mwebManager = manager

        bitcoinCore.addMessageParser(GetMwebUtxosMessageParser())
        bitcoinCore.addMessageSerializer(GetMwebUtxosMessageSerializer())
        bitcoinCore.addMessageParser(MwebUtxosMessageParser())
        bitcoinCore.addMessageSerializer(MwebUtxosMessageSerializer())
        bitcoinCore.addMessageParser(MwebTransactionMessageParser())
        bitcoinCore.addMessageSerializer(MwebTransactionMessageSerializer())
        bitcoinCore.addPeerSyncListener(manager)
        bitcoinCore.addPeerTaskHandler(manager)
    }

    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context
     * @param watchAddress address for watching in read-only mode
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     */
    constructor(
        context: Context,
        watchAddress: String,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) {
        network = network(networkType)

        val address = parseAddress(watchAddress, network)
        val watchAddressPublicKey = WatchAddressPublicKey(address.lockingScriptPayload, address.scriptType)
        val purpose = address.scriptType.purpose ?: throw IllegalStateException("Not supported scriptType ${address.scriptType}")

        bitcoinCore = bitcoinCore(
            context = context,
            extendedKey = null,
            watchAddressPublicKey = watchAddressPublicKey,
            networkType = networkType,
            walletId = walletId,
            syncMode = syncMode,
            purpose = purpose,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )

        val mwebDb = MwebDatabase.getInstance(context, getMwebDatabaseName(networkType, walletId))
        val storage = MwebStorage(mwebDb)
        this.mwebStorage = storage
        val manager = MwebManager(
            mwebStorage = storage,
            scanner = null,
            lastBlockHashProvider = { bitcoinCore.lastBlockInfo?.headerHash?.toReversedByteArray() }
        )
        mwebManager = manager

        bitcoinCore.addMessageParser(GetMwebUtxosMessageParser())
        bitcoinCore.addMessageSerializer(GetMwebUtxosMessageSerializer())
        bitcoinCore.addMessageParser(MwebUtxosMessageParser())
        bitcoinCore.addMessageSerializer(MwebUtxosMessageSerializer())
        bitcoinCore.addMessageParser(MwebTransactionMessageParser())
        bitcoinCore.addMessageSerializer(MwebTransactionMessageSerializer())
        bitcoinCore.addPeerSyncListener(manager)
        bitcoinCore.addPeerTaskHandler(manager)
    }

    private fun bitcoinCore(
        context: Context,
        extendedKey: HDExtendedKey?,
        watchAddressPublicKey: WatchAddressPublicKey?,
        networkType: NetworkType,
        walletId: String,
        syncMode: SyncMode,
        purpose: Purpose,
        peerSize: Int,
        confirmationsThreshold: Int
    ): BitcoinCore {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, purpose))
        val storage = Storage(database)
        val checkpoint = Checkpoint.resolveCheckpoint(syncMode, network, storage)
        val apiSyncStateManager = ApiSyncStateManager(storage, network.syncableFromApi && syncMode !is SyncMode.Full)
        val blockchairApi = BlockchairApi(network.blockchairChainId)
        val apiTransactionProvider = apiTransactionProvider(networkType, blockchairApi)
        val paymentAddressParser = PaymentAddressParser("litecoin", removeScheme = true)
        val blockValidatorSet = blockValidatorSet(storage, networkType)

        val coreBuilder = BitcoinCoreBuilder()

        val bitcoinCore = coreBuilder
            .setContext(context)
            .setExtendedKey(extendedKey)
            .setWatchAddressPublicKey(watchAddressPublicKey)
            .setPurpose(purpose)
            .setNetwork(network)
            .setCheckpoint(checkpoint)
            .setPaymentAddressParser(paymentAddressParser)
            .setPeerSize(peerSize)
            .setSyncMode(syncMode)
            .setSendType(BitcoinCore.SendType.API(blockchairApi))
            .setConfirmationThreshold(confirmationsThreshold)
            .setStorage(storage)
            .setApiTransactionProvider(apiTransactionProvider)
            .setApiSyncStateManager(apiSyncStateManager)
            .setBlockValidator(blockValidatorSet)
            .build()

        //  extending bitcoinCore

        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        // MWEB stealth address support — prepended last so it is tried first in the chain
        val mwebHrp = if (networkType == NetworkType.MainNet) "ltcmweb" else "tmweb"
        bitcoinCore.prependAddressConverter(MwebAddressConverter(mwebHrp))

        when (purpose) {
            Purpose.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
            }

            Purpose.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }

            Purpose.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(SegwitAddressConverter(network.addressSegwitHrp)))
            }

            Purpose.BIP86 -> {
                bitcoinCore.addRestoreKeyConverter(Bip86RestoreKeyConverter(SegwitAddressConverter(network.addressSegwitHrp)))
            }
        }

        return bitcoinCore
    }

    private fun parseAddress(address: String, network: Network): Address {
        val addressConverter = AddressConverterChain().apply {
            prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
            prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
        }
        return addressConverter.convert(address)
    }

    private fun blockValidatorSet(
        storage: Storage,
        networkType: NetworkType
    ): BlockValidatorSet {
        val blockValidatorSet = BlockValidatorSet()

        val proofOfWorkValidator = ProofOfWorkValidator(ScryptHasher())
        blockValidatorSet.addBlockValidator(proofOfWorkValidator)

        val blockValidatorChain = BlockValidatorChain()

        val blockHelper = BlockValidatorHelper(storage)

        if (networkType == NetworkType.MainNet) {
            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            blockValidatorChain.add(BitsValidator())
        } else if (networkType == NetworkType.TestNet) {
            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            blockValidatorChain.add(LegacyTestNetDifficultyValidator(storage, heightInterval, targetSpacing, maxTargetBits))
            blockValidatorChain.add(BitsValidator())
        }

        blockValidatorSet.addBlockValidator(blockValidatorChain)
        return blockValidatorSet
    }

    private fun apiTransactionProvider(
        networkType: NetworkType,
        blockchairApi: BlockchairApi
    ) = when (networkType) {
        NetworkType.MainNet -> {
            val blockchairBlockHashFetcher = BlockchairBlockHashFetcher(blockchairApi)
            BlockchairTransactionProvider(blockchairApi, blockchairBlockHashFetcher)
        }

        NetworkType.TestNet -> {
            BCoinApi("")
        }
    }

    companion object {

        const val maxTargetBits: Long = 0x1e0fffff      // Maximum difficulty
        const val targetSpacing = 150                   // 2.5 minutes per block.
        const val targetTimespan: Long = 302400         // 3.5 days per difficulty cycle, on average.
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks

        val defaultNetworkType: NetworkType = NetworkType.MainNet
        val defaultSyncMode: SyncMode = SyncMode.Api()
        const val defaultPeerSize: Int = 10
        const val defaultConfirmationsThreshold: Int = 6

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, purpose: Purpose): String =
            "Litecoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${purpose.name}"

        private fun getMwebDatabaseName(networkType: NetworkType, walletId: String): String =
            "Litecoin-MWEB-${networkType.name}-$walletId"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.Blockchair())) {
                for (purpose in Purpose.values())
                    try {
                        SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, purpose)))
                    } catch (ex: Exception) {
                        continue
                    }
            }
            SQLiteDatabase.deleteDatabase(context.getDatabasePath(getMwebDatabaseName(networkType, walletId)))
        }

        private fun network(networkType: NetworkType) = when (networkType) {
            NetworkType.MainNet -> MainNetLitecoin()
            NetworkType.TestNet -> TestNetLitecoin()
        }

        private fun addressConverter(purpose: Purpose, network: Network): AddressConverterChain {
            val addressConverter = AddressConverterChain()
            when (purpose) {
                Purpose.BIP44,
                Purpose.BIP49 -> {
                    addressConverter.prependConverter(
                        Base58AddressConverter(network.addressVersion, network.addressScriptVersion)
                    )
                }

                Purpose.BIP84,
                Purpose.BIP86 -> {
                    addressConverter.prependConverter(
                        SegwitAddressConverter(network.addressSegwitHrp)
                    )
                }
            }

            return addressConverter
        }

        fun firstAddress(
            seed: ByteArray,
            purpose: Purpose,
            networkType: NetworkType = NetworkType.MainNet,
        ): Address {
            return BitcoinCore.firstAddress(
                seed,
                purpose,
                network(networkType),
                addressConverter(purpose, network(networkType))
            )
        }

        fun firstAddress(
            extendedKey: HDExtendedKey,
            purpose: Purpose,
            networkType: NetworkType = NetworkType.MainNet,
        ): Address {
            return BitcoinCore.firstAddress(
                extendedKey,
                purpose,
                network(networkType),
                addressConverter(purpose, network(networkType))
            )
        }
    }

}
