package io.horizontalsystems.bitcoincash

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincash.blocks.BitcoinCashBlockValidatorHelper
import io.horizontalsystems.bitcoincash.blocks.validators.DAAValidator
import io.horizontalsystems.bitcoincash.blocks.validators.EDAValidator
import io.horizontalsystems.bitcoincash.blocks.validators.ForkValidator
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.BlockMedianTimeHelper
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.blocks.validators.LegacyDifficultyAdjustmentValidator
import io.horizontalsystems.bitcoincore.blocks.validators.ProofOfWorkValidator
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.managers.Bip44RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.InsightApi
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
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

    constructor(
            context: Context,
            words: List<String>,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6
    ) : this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    constructor(
            context: Context,
            seed: ByteArray,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6
    ) {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode))
        val storage = Storage(database)
        val initialSyncUrl: String

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://cashexplorer.bitcoin.com/api"
                MainNetBitcoinCash()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = "https://tbch.blockdozer.com/api"
                TestNetBitcoinCash()
            }
        }

        val paymentAddressParser = PaymentAddressParser("bitcoincash", removeScheme = false)
        val initialSyncApi = InsightApi(initialSyncUrl)

        val blockValidatorSet = BlockValidatorSet()
        blockValidatorSet.addBlockValidator(ProofOfWorkValidator())

        val blockValidatorChain = BlockValidatorChain()
        if (networkType == NetworkType.MainNet) {
            val blockHelper = BitcoinCashBlockValidatorHelper(storage)

            val daaValidator = DAAValidator(targetSpacing, blockHelper)
            blockValidatorChain.add(ForkValidator(svForkHeight, abcForkBlockHash, daaValidator))
            blockValidatorChain.add(daaValidator)
            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits))
            blockValidatorChain.add(EDAValidator(maxTargetBits, blockHelper, network.bip44CheckpointBlock.height, BlockMedianTimeHelper(storage)))
        }

        blockValidatorSet.addBlockValidator(blockValidatorChain)

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setInitialSyncApi(initialSyncApi)
                .setBlockValidator(blockValidatorSet)
                .build()

        //  extending bitcoinCore

        val bech32 = CashAddressConverter(network.addressSegwitHrp)
        val base58 = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32)

        bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58))
    }

    fun transactions(fromUid: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromUid, limit)
    }

    companion object {
        val maxTargetBits: Long = 0x1d00ffff                // Maximum difficulty
        val targetSpacing = 10 * 60                         // 10 minutes per block.
        val targetTimespan: Long = 14 * 24 * 60 * 60        // 2 weeks per difficulty cycle, on average.
        var heightInterval = targetTimespan / targetSpacing // 2016 blocks
        val svForkHeight = 556767
        val abcForkBlockHash = "0000000000000000004626ff6e3b936941d341c5932ece4357eeccac44e6d56c".toReversedByteArray()

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode): String = "BitcoinCash-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                try {
                    SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode)))
                } catch (ex: Exception) {
                    continue
                }
            }
        }
    }

}
