package io.horizontalsystems.litecoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.managers.BCoinApi
import io.horizontalsystems.bitcoincore.managers.Bip44RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip49RestoreKeyConverter
import io.horizontalsystems.bitcoincore.managers.Bip84RestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.litecoinkit.validators.ProofOfWorkValidator
import io.reactivex.Single

class LitecoinKit : AbstractKit {
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
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) : this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, syncMode, confirmationsThreshold, bip)

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
        var initialSyncUrl = ""

        // TODO need to update initialSyncUrl with actual api url
        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://ltc.horizontalsystems.xyz/apg"
                MainNet()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = "http://ltc-testnet.horizontalsystems.xyz/apg"
                TestNet()
            }
        }

        val paymentAddressParser = PaymentAddressParser("litecoin", removeScheme = true)
        val initialSyncApi = BCoinApi(initialSyncUrl)

        // TODO need to implement block validators and set them
        val blockValidatorSet = BlockValidatorSet()

        val proofOfWorkValidator = ProofOfWorkValidator(ScryptHasher())
        blockValidatorSet.addBlockValidator(proofOfWorkValidator)

        val blockValidatorChain = BlockValidatorChain()

//        val blockHelper = BlockValidatorHelper(storage)

//        if (networkType == NetworkType.MainNet) {
//            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
//            blockValidatorChain.add(BitsValidator())
//        } else if (networkType == NetworkType.TestNet) {
//            blockValidatorChain.add(LegacyDifficultyAdjustmentValidator(blockHelper, BitcoinCore.heightInterval, BitcoinCore.targetTimespan, BitcoinCore.maxTargetBits))
//            blockValidatorChain.add(LegacyTestNetDifficultyValidator(storage, BitcoinCore.heightInterval, BitcoinCore.targetSpacing, BitcoinCore.maxTargetBits))
//            blockValidatorChain.add(BitsValidator())
//        }

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

    fun transactions(fromUid: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromUid, limit)
    }

    companion object {

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, bip: Bip): String = "Litecoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${bip.name}"

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
