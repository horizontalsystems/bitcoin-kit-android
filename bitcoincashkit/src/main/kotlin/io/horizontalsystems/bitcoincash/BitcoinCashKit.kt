package io.horizontalsystems.bitcoincash

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoincash.blocks.validators.DAAValidator
import io.horizontalsystems.bitcoincash.blocks.validators.EDAValidator
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoincore.managers.BCoinApi
import io.horizontalsystems.bitcoincore.managers.BitcoinCashAddressSelector
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single

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
            bitcoinCore.listener = value
        }

    constructor(context: Context, words: List<String>, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api(), confirmationsThreshold: Int = 6) :
            this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    constructor(context: Context, seed: ByteArray, walletId: String, networkType: NetworkType = NetworkType.MainNet, peerSize: Int = 10, syncMode: BitcoinCore.SyncMode = BitcoinCore.SyncMode.Api(), confirmationsThreshold: Int = 6) {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId))
        val storage = Storage(database)
        val initialSyncUrl: String

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://bch.horizontalsystems.xyz/apg"
                MainNetBitcoinCash()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = "http://bch-testnet.horizontalsystems.xyz/apg"
                TestNetBitcoinCash()
            }
        }

        val paymentAddressParser = PaymentAddressParser("bitcoincash", removeScheme = false)
        val addressSelector = BitcoinCashAddressSelector()
        val initialSyncApi = BCoinApi(initialSyncUrl)

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setAddressSelector(addressSelector)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setInitialSyncApi(initialSyncApi)
                .build()

        //  extending bitcoinCore

        val bech32 = CashAddressConverter(network.addressSegwitHrp)
        bitcoinCore.prependAddressConverter(bech32)

        if (networkType == NetworkType.MainNet) {
            val blockHelper = BitcoinCashBlockValidatorHelper(storage)

            bitcoinCore.addBlockValidator(DAAValidator(targetSpacing, blockHelper))
            bitcoinCore.addBlockValidator(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            bitcoinCore.addBlockValidator(EDAValidator(maxTargetBits, blockHelper, network.bip44CheckpointBlock.height))
        }
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromHash, limit)
    }

    companion object {
        val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty

        val targetSpacing = 10 * 60                         // 10 minutes per block.
        val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        var heightInterval = targetTimespan / targetSpacing // 2016 blocks


        private fun getDatabaseName(networkType: NetworkType, walletId: String): String = "BitcoinCash-${networkType.name}-$walletId"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId)))
        }
    }

}
