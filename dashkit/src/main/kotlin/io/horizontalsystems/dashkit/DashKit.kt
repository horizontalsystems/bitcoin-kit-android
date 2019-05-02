package io.horizontalsystems.dashkit

import android.content.Context
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.managers.BitcoinAddressSelector
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.MerkleBranch
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.dashkit.managers.MasternodeListManager
import io.horizontalsystems.dashkit.managers.MasternodeListSyncer
import io.horizontalsystems.dashkit.managers.MasternodeSortedList
import io.horizontalsystems.dashkit.masternodelist.MasternodeCbTxHasher
import io.horizontalsystems.dashkit.masternodelist.MasternodeListMerkleRootCalculator
import io.horizontalsystems.dashkit.masternodelist.MerkleRootCreator
import io.horizontalsystems.dashkit.masternodelist.MerkleRootHasher
import io.horizontalsystems.dashkit.messages.GetMasternodeListDiffMessageSerializer
import io.horizontalsystems.dashkit.messages.MasternodeListDiffMessageParser
import io.horizontalsystems.dashkit.messages.TransactionLockMessageParser
import io.horizontalsystems.dashkit.messages.TransactionLockVoteMessageParser
import io.horizontalsystems.dashkit.models.CoinbaseTransactionSerializer
import io.horizontalsystems.dashkit.models.MasternodeSerializer
import io.horizontalsystems.dashkit.storage.DashKitDatabase
import io.horizontalsystems.dashkit.storage.DashStorage
import io.horizontalsystems.dashkit.tasks.PeerTaskFactory
import io.horizontalsystems.dashkit.validators.DarkGravityWaveTestnetValidator
import io.horizontalsystems.dashkit.validators.DarkGravityWaveValidator
import io.horizontalsystems.hdwalletkit.Mnemonic

class DashKit : AbstractKit {
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
        val databaseName = "${javaClass.simpleName}-${networkType.name}-$walletId"
        val coreDatabase = CoreDatabase.getInstance(context, "$databaseName-core")
        val dashDatabase = DashKitDatabase.getInstance(context, databaseName)

        val coreStorage = Storage(coreDatabase)
        val dashStorage = DashStorage(dashDatabase, coreStorage)

        network = when (networkType) {
            NetworkType.MainNet -> MainNetDash()
            NetworkType.TestNet -> TestNetDash()
        }

        val paymentAddressParser = PaymentAddressParser("bitcoin", removeScheme = true)

        val addressSelector = BitcoinAddressSelector()

        bitcoinCore = BitcoinCoreBuilder()
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setPaymentAddressParser(paymentAddressParser)
                .setAddressSelector(addressSelector)
                .setApiFeeRateCoinCode("DASH")
                .setPeerSize(peerSize)
                .setNewWallet(newWallet)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(coreStorage)
                .setBlockHeaderHasher(X11Hasher())
                .build()

        //  extending bitcoinCore
        val blockHelper = BlockValidatorHelper(coreStorage)

        if (network is MainNetDash) {
            bitcoinCore.addBlockValidator(DarkGravityWaveValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits, network.checkpointBlock.height))
        } else {
            bitcoinCore.addBlockValidator(DarkGravityWaveTestnetValidator(targetSpacing, targetTimespan, maxTargetBits))
            bitcoinCore.addBlockValidator(DarkGravityWaveValidator(blockHelper, heightInterval, targetTimespan, maxTargetBits, network.checkpointBlock.height))
        }

        bitcoinCore.addMessageParser(MasternodeListDiffMessageParser())
                .addMessageParser(TransactionLockMessageParser())
                .addMessageParser(TransactionLockVoteMessageParser())

        bitcoinCore.addMessageSerializer(GetMasternodeListDiffMessageSerializer())

        val merkleRootHasher = MerkleRootHasher()
        val merkleRootCreator = MerkleRootCreator(merkleRootHasher)
        val masternodeListMerkleRootCalculator = MasternodeListMerkleRootCalculator(MasternodeSerializer(), merkleRootHasher, merkleRootCreator)
        val masternodeCbTxHasher = MasternodeCbTxHasher(CoinbaseTransactionSerializer(), merkleRootHasher)

        val masternodeListManager = MasternodeListManager(dashStorage, masternodeListMerkleRootCalculator, masternodeCbTxHasher, MerkleBranch(), MasternodeSortedList())
        val masternodeSyncer = MasternodeListSyncer(bitcoinCore, PeerTaskFactory(), masternodeListManager, bitcoinCore.initialBlockDownload)

        bitcoinCore.addPeerTaskHandler(masternodeSyncer)
        bitcoinCore.addPeerSyncListener(masternodeSyncer)
        bitcoinCore.addPeerGroupListener(masternodeSyncer)

        val instantSend = InstantSend(bitcoinCore.transactionSyncer)
        bitcoinCore.addInventoryItemsHandler(instantSend)
        bitcoinCore.addPeerTaskHandler(instantSend)
    }

    companion object {
        const val maxTargetBits: Long = 0x1e0fffff

        const val targetSpacing = 150             // 2.5 min. for mining 1 Block
        const val targetTimespan = 3600L          // 1 hour for 24 blocks
        const val heightInterval = targetTimespan / targetSpacing
    }
}
