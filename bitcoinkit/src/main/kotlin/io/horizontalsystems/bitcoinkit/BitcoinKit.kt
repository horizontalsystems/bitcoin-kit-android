package io.horizontalsystems.bitcoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.blocks.validators.*
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IInitialSyncApi
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic
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
        MainNet,
        TestNet,
        RegTest
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
     * @param bip which BIP algorithm to use for wallet generation. Default is BIP44.
     */
    constructor(
            context: Context,
            words: List<String>,
            passphrase: String,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44

    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, peerSize, syncMode, confirmationsThreshold, bip)



    /**
     * @constructor Creates and initializes the BitcoinKit
     * @param context The Android context
     * @param seed A byte array that contains the seedphrase.
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Api().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     * @param bip which BIP algorithm to use for wallet generation. The default is BIP44.
     */

    constructor(
            context: Context,
            seed: ByteArray,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, bip))
        val storage = Storage(database)
        val initialSyncApi: IInitialSyncApi

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncApi = InsightApi("https://explorer.api.bitcoin.com/btc/v1")
                MainNet()
            }
            NetworkType.TestNet -> {
                initialSyncApi = BCoinApi("https://btc-testnet.horizontalsystems.xyz/api")
                TestNet()
            }
            NetworkType.RegTest -> {
                initialSyncApi = InsightApi("")
                RegTest()
            }
        }

        val paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)
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

        val coreBuilder = BitcoinCoreBuilder()

        bitcoinCore = coreBuilder
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setBip(bip)
                .setPaymentAddressParser(paymentAddressParser)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setInitialSyncApi(initialSyncApi)
                .setBlockValidator(blockValidatorSet)
                .setHandleAddrMessage(false)
                .addPlugin(HodlerPlugin(coreBuilder.addressConverter, storage, BlockMedianTimeHelper(storage)))
                .build()

        //  extending bitcoinCore

        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        when (bip) {
            Bip.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }
            Bip.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }
            Bip.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }
        }
    }

    companion object {
        const val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty
        const val targetSpacing = 10 * 60                         // 10 minutes per block.
        const val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks

        /**
         * Gets the name of the BitcoinKit database
         * @param networkType The network type (MAIN, TEST, or REG)
         * @param walletId The walletID
         * @param syncMode The SyncMode
         * @param bip The BIP
         * @return database name
         */

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, bip: Bip): String = "Bitcoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${bip.name}"

        /**
         * Clears the database
         * @param context The context of the BitcoinKit.
         * @param networkType The networkType of the BitcoinKit.
         * @param walletId The string wallet ID of the BitcoinKit.
         */
        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                for (bip in Bip.values())
                    try {
                        SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, bip)))
                    } catch (ex: Exception) {
                        continue
                    }
            }
        }
    }

}
