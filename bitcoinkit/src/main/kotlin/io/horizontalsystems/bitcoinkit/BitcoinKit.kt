package io.horizontalsystems.bitcoinkit

import android.arch.persistence.room.Room
import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.validators.BitsValidator
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyTestNetDifficultyValidator
import io.horizontalsystems.bitcoincore.managers.BitcoinAddressSelector
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.KitDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
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
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(BitsValidator())
        } else if (networkType == NetworkType.TestNet) {
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(LegacyTestNetDifficultyValidator(storage, BitcoinCore.heightInterval, BitcoinCore.targetSpacing, BitcoinCore.maxTargetBits))
            bitcoinCore.addBlockValidator(BitsValidator())
        }
    }
}
