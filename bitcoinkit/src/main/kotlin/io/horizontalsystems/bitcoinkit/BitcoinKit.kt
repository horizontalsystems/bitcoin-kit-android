package io.horizontalsystems.bitcoinkit

import android.arch.persistence.room.Room
import android.content.Context
import io.horizontalsystems.bitcoinkit.blocks.validators.BitsValidator
import io.horizontalsystems.bitcoinkit.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoinkit.blocks.validators.LegacyTestNetDifficultyValidator
import io.horizontalsystems.bitcoinkit.managers.BitcoinAddressSelector
import io.horizontalsystems.bitcoinkit.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.RegTest
import io.horizontalsystems.bitcoinkit.network.TestNet
import io.horizontalsystems.bitcoinkit.storage.KitDatabase
import io.horizontalsystems.bitcoinkit.storage.Storage
import io.horizontalsystems.bitcoinkit.utils.PaymentAddressParser
import io.horizontalsystems.bitcoinkit.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic

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
            value?.let { bitcoinCore.addListener(it) }
        }

    constructor(context: Context, words: List<String>, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, newWallet: Boolean = false, confirmationsThreshold: Int = 6) :
            this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, newWallet, confirmationsThreshold)

    constructor(context: Context, seed: ByteArray, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, newWallet: Boolean = false, confirmationsThreshold: Int = 6) {
        val databaseName = "${this.javaClass.simpleName}-${networkType.name}-$walletId"

        val database = Room.databaseBuilder(context, KitDatabase::class.java, databaseName)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .addMigrations()
                .build()

        val storage = Storage(database)

        network = when (networkType) {
            NetworkType.MainNet -> MainNet()
            NetworkType.TestNet -> TestNet()
            NetworkType.RegTest -> RegTest()
        }

        val paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)

        val addressSelector = BitcoinAddressSelector()

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setAddressSelector(addressSelector)
                .setApiFeeRateCoinCode("BTC")
                .setPeerSize(peerSize)
                .setNewWallet(newWallet)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .build()


        // extending bitcoinCore

        val bech32 = SegwitAddressConverter(network.addressSegwitHrp)
        bitcoinCore.prependAddressConverter(bech32)

        val blockHelper = BlockValidatorHelper(storage)

        if (networkType == NetworkType.MainNet) {
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(network, blockHelper))
            bitcoinCore.addBlockValidator(BitsValidator())
        }
        else if (networkType == NetworkType.TestNet) {
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(network, blockHelper))
            bitcoinCore.addBlockValidator(LegacyTestNetDifficultyValidator(network, storage))
            bitcoinCore.addBlockValidator(BitsValidator())
        }
    }
}
