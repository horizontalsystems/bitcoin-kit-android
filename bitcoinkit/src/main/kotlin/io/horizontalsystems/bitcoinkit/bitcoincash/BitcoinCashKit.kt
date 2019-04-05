package io.horizontalsystems.bitcoinkit.bitcoincash

import android.arch.persistence.room.Room
import android.content.Context
import io.horizontalsystems.bitcoinkit.AbstractKit
import io.horizontalsystems.bitcoinkit.BitcoinCore
import io.horizontalsystems.bitcoinkit.BitcoinCoreBuilder
import io.horizontalsystems.bitcoinkit.blocks.validators.BitcoinCashValidator
import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetBitcoinCashValidator
import io.horizontalsystems.bitcoinkit.managers.BitcoinCashAddressSelector
import io.horizontalsystems.bitcoinkit.network.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.TestNetBitcoinCash
import io.horizontalsystems.bitcoinkit.storage.KitDatabase
import io.horizontalsystems.bitcoinkit.storage.Storage
import io.horizontalsystems.bitcoinkit.utils.CashAddressConverter
import io.horizontalsystems.bitcoinkit.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.Mnemonic

class BitcoinCashKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    override var bitcoinCore: BitcoinCore
    override var network: Network

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

        val blockValidator = when (networkType) {
            NetworkType.MainNet -> BitcoinCashValidator(network, storage)
            NetworkType.TestNet -> TestnetBitcoinCashValidator(network, storage)
        }
        bitcoinCore.addBlockValidator(blockValidator)
    }

}