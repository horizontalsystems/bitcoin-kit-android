package io.horizontalsystems.bitcoincash

import android.arch.persistence.room.Room
import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoincash.blocks.validators.DAAValidator
import io.horizontalsystems.bitcoincash.blocks.validators.EDAValidator
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoincore.managers.BitcoinCashAddressSelector
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.KitDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.Mnemonic

class BitcoinCashKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet
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
            NetworkType.MainNet -> MainNetBitcoinCash()
            NetworkType.TestNet -> TestNetBitcoinCash()
        }

        val paymentAddressParser = PaymentAddressParser("bitcoincash", removeScheme = false)

        val addressSelector = BitcoinCashAddressSelector()

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setAddressSelector(addressSelector)
                .setApiFeeRateCoinCode("BCH")
                .setPeerSize(peerSize)
                .setNewWallet(newWallet)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .build()


        // extending bitcoinCore

        val bech32 = CashAddressConverter(network.addressSegwitHrp)
        bitcoinCore.prependAddressConverter(bech32)

        if (networkType == NetworkType.MainNet) {
            val blockHelper = BitcoinCashBlockValidatorHelper(storage)

            bitcoinCore.addBlockValidator(DAAValidator(targetSpacing, blockHelper))
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            bitcoinCore.addBlockValidator(EDAValidator(maxTargetBits, blockHelper))
        }
    }

    companion object {
        val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty

        val targetSpacing = 10 * 60                         // 10 minutes per block.
        val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        var heightInterval = targetTimespan / targetSpacing // 2016 blocks
    }

}
