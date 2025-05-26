package io.horizontalsystems.bitcoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.apisync.BCoinApi
import io.horizontalsystems.bitcoincore.apisync.BlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.BlockchainComApi
import io.horizontalsystems.bitcoincore.apisync.HsBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApi
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairTransactionProvider
import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.blocks.validators.*
import io.horizontalsystems.bitcoincore.core.purpose
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.WatchAddressPublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.bitcoinkit.BitcoinKit.Listener
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.hdwalletkit.*
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.hodler.HodlerPlugin

/**
 *
 *
 * The kit that connects to the Bitcoin Network and creates the Bitcoin wallet.
 * Extends from the AbstractKit class.
 * @property NetworkType The enum class type that determines which bitcoin network the kit is connects to. (MainNet, TestNet, or RegTest)
 * @property Listener Interface of BitcoinCore.Listener
 * @property bitcoinCore Reference to the BitcoinCore class.
 * @property network  The type of network that this kit is connected to. It is determined by the NetWorkType enum class.
 * @property listener Changeable variable of BitcoinCore.Listener.
 *
 */
class BitcoinKit : AbstractKit {

    enum class NetworkType {
        MainNet, TestNet, RegTest
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context.
     * @param words A list of words of type String.
     * @param passphrase The passphrase to the wallet.
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. Default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. Default is 6 confirmations.
     * @param purpose which BIP algorithm to use for wallet generation. Default is BIP44.
     */
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


    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context.
     * @param seed A byte array that contains the seed.
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. Default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. Default is 6 confirmations.
     * @param purpose which BIP algorithm to use for wallet generation. Default is BIP44.
     */
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
     * @param purpose Used for HDKey derivation
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

        bitcoinCore = bitcoinCore(
            context = context,
            extendedKey = extendedKey,
            watchAddressPublicKey = null,
            networkType = networkType,
            network = network,
            walletId = walletId,
            syncMode = syncMode,
            purpose = purpose,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )
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
            purpose = purpose,
            networkType = networkType,
            network = network,
            walletId = walletId,
            syncMode = syncMode,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )
    }

    private fun bitcoinCore(
        context: Context,
        extendedKey: HDExtendedKey?,
        watchAddressPublicKey: WatchAddressPublicKey?,
        purpose: Purpose,
        networkType: NetworkType,
        network: Network,
        walletId: String,
        syncMode: SyncMode,
        peerSize: Int,
        confirmationsThreshold: Int
    ): BitcoinCore {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, purpose))
        val storage = Storage(database)

        val checkpoint = Checkpoint.resolveCheckpoint(syncMode, network, storage)
        val apiSyncStateManager = ApiSyncStateManager(storage, network.syncableFromApi && syncMode !is SyncMode.Full)
        val apiTransactionProvider = apiTransactionProvider(networkType, syncMode, checkpoint)
        val paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)
        val blockValidatorSet = blockValidatorSet(networkType, storage)

        val coreBuilder = BitcoinCoreBuilder()
        val hodlerPlugin = hodlerPlugin(storage, syncMode, coreBuilder.addressConverter)

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
            .setConfirmationThreshold(confirmationsThreshold)
            .setStorage(storage)
            .setApiTransactionProvider(apiTransactionProvider)
            .setApiSyncStateManager(apiSyncStateManager)
            .setBlockValidator(blockValidatorSet)
            .setHandleAddrMessage(false)
            .addPlugin(hodlerPlugin)
            .build()

        //  extending bitcoinCore
        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        when (purpose) {
            Purpose.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
                bitcoinCore.addRestoreKeyConverter(hodlerPlugin)

                if (extendedKey != null) {
                    bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
                    bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
                }
            }

            Purpose.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }

            Purpose.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }

            Purpose.BIP86 -> {
                bitcoinCore.addRestoreKeyConverter(Bip86RestoreKeyConverter(bech32AddressConverter))
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

    private fun hodlerPlugin(
        storage: Storage,
        syncMode: SyncMode,
        addressConverter: IAddressConverter
    ): HodlerPlugin {
        val blockMedianTimeHelper = BlockMedianTimeHelper(storage, approximate = syncMode is SyncMode.Blockchair)
        return HodlerPlugin(addressConverter, storage, blockMedianTimeHelper)
    }

    private fun blockValidatorSet(
        networkType: NetworkType,
        storage: Storage
    ): BlockValidatorSet {
        val blockHelper = BlockValidatorHelper(storage)
        val blockValidatorSet = BlockValidatorSet()

        blockValidatorSet.addBlockValidator(ProofOfWorkValidator())

        val blockValidatorChain = BlockValidatorChain()
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
        syncMode: SyncMode,
        checkpoint: Checkpoint
    ) = when (networkType) {
        NetworkType.MainNet -> {
            val hsBlockHashFetcher = HsBlockHashFetcher("https://api.blocksdecoded.com/v1/blockchains/bitcoin")
            if (syncMode is SyncMode.Blockchair) {
                val blockchairApi = BlockchairApi(network.blockchairChainId)
                val blockchairBlockHashFetcher = BlockchairBlockHashFetcher(blockchairApi)
                val blockHashFetcher = BlockHashFetcher(hsBlockHashFetcher, blockchairBlockHashFetcher, checkpoint.block.height)
                val blockchairProvider = BlockchairTransactionProvider(blockchairApi, blockHashFetcher)
                blockchairProvider
            } else {
                BlockchainComApi("https://blockchain.info", hsBlockHashFetcher)
            }
        }

        NetworkType.TestNet -> {
            BCoinApi("https://btc-testnet.blocksdecoded.com/api")
        }

        NetworkType.RegTest -> {
            null
        }
    }

    companion object {
        const val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty
        const val targetSpacing = 10 * 60                         // 10 minutes per block.
        const val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks

        val defaultNetworkType: NetworkType = NetworkType.MainNet
        val defaultSyncMode: SyncMode = SyncMode.Api()
        const val defaultPeerSize: Int = 10
        const val defaultConfirmationsThreshold: Int = 6

        /**
         * Gets the name of the BitcoinKit database
         * @param networkType The network type (MAIN, TEST, or REG)
         * @param walletId The walletID
         * @param syncMode The SyncMode
         * @param bip The BIP
         * @return database name
         */

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, purpose: Purpose): String =
            "Bitcoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${purpose.name}"

        /**
         * Clears the database
         * @param context The context of the BitcoinKit.
         * @param networkType The networkType of the BitcoinKit.
         * @param walletId The string wallet ID of the BitcoinKit.
         */
        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.Blockchair())) {
                for (purpose in Purpose.values()) try {
                    SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, purpose)))
                } catch (ex: Exception) {
                    continue
                }
            }
        }

        private fun network(networkType: NetworkType) = when (networkType) {
            NetworkType.MainNet -> MainNet()
            NetworkType.TestNet -> TestNet()
            NetworkType.RegTest -> RegTest()
        }

        private fun addressConverter(purpose: Purpose, network: Network): AddressConverterChain {
            val addressConverter = AddressConverterChain()
            when (purpose) {
                Purpose.BIP44,
                Purpose.BIP49,
                    -> {
                    addressConverter.prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }
                Purpose.BIP84,
                Purpose.BIP86,
                    -> {
                    addressConverter.prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                }
            }

            return addressConverter
        }

        fun firstAddress(
            seed: ByteArray,
            purpose: Purpose,
            networkType: NetworkType,
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
            networkType: NetworkType,
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
