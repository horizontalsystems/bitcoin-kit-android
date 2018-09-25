package bitcoin.wallet.kit

import android.content.Context
import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.Mnemonic
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.*
import bitcoin.wallet.kit.models.*
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.network.PeerManager
import bitcoin.wallet.kit.scripts.ScriptType
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class WalletKitModule

class WalletKit(words: List<String>) {

    interface Listener {
        fun transactionsUpdated(walletKit: WalletKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun balanceUpdated(walletKit: WalletKit, balance: Long)
        fun lastBlockHeightUpdated(walletKit: WalletKit, lastBlockHeight: Int)
        fun progressUpdated(walletKit: WalletKit, progress: Double)
    }

    var listener: Listener? = null

    val transactions: List<TransactionInfo>
        get() = transactionRealmResults.mapNotNull { transactionInfo(it) }

    val lastBlockHeight: Int
        get() = blockRealmResults.lastOrNull()?.height ?: 0

    val balance: Long
        get() = transactionOutputRealmResults.filter { it.inputs?.size ?: 0 == 0 }.map { it.value }.sum()

    private val initialSyncer: InitialSyncer
    private val transactionRealmResults: RealmResults<Transaction>

    // we use transactionOutputRealmResults instead of unspentOutputRealmResults
    // since Realm java does not support Collection aggregate queries as in Swift
    private val transactionOutputRealmResults: RealmResults<TransactionOutput>
    private val blockRealmResults: RealmResults<Block>

    init {
        val realmFactory = RealmFactory(getRealmConfig())
        val realm = realmFactory.realm

        val network = MainNet()
        val wallet = HDWallet(Mnemonic().toSeed(words), network)
        val pubKeys = realm.where(PublicKey::class.java).findAll()
        val filters = BloomFilter(pubKeys.size)

        pubKeys.forEach {
            filters.insert(it.publicKey)
        }

        val peerManager = PeerManager(network)

        val peerGroup = PeerGroup(peerManager, network, 1)
        peerGroup.setBloomFilter(filters)
        peerGroup.listener = Syncer(realmFactory, peerGroup, network)

        val apiManager = ApiManager("http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/blockstore")
        val stateManager = StateManager(realmFactory)

        val blockDiscover = BlockDiscover(wallet, apiManager, network)

        initialSyncer = InitialSyncer(realmFactory, blockDiscover, stateManager, peerGroup)

        transactionRealmResults = realm.where(Transaction::class.java)
                .equalTo("isMine", true)
                .findAll()

        transactionRealmResults.addChangeListener { t, changeSet ->
            handleTransactions(t, changeSet)
        }

        transactionOutputRealmResults = realm.where(TransactionOutput::class.java)
                .isNotNull("publicKey")
                .`in`("scriptType", arrayOf(ScriptType.P2PKH, ScriptType.P2PK))
                .findAll()

        transactionOutputRealmResults.addChangeListener { t, changeSet ->
            handleUnspentOutputs(changeSet)
        }

        blockRealmResults = realm.where(Block::class.java)
                .sort("height")
                .findAll()

        blockRealmResults.addChangeListener { t, changeSet ->
            handleBlocks(t, changeSet)
        }
    }

    fun start() {
        initialSyncer.sync()
    }

    private fun getRealmConfig(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .name("kit")
                .deleteRealmIfMigrationNeeded()
                .modules(WalletKitModule())
                .build()
    }

    private fun handleTransactions(collection: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            listener?.let { listener ->
                val inserted = changeSet.insertions.asList().mapNotNull { transactionInfo(collection[it]) }
                val updated = changeSet.changes.asList().mapNotNull { transactionInfo(collection[it]) }
                val deleted = changeSet.deletions.asList()

                listener.transactionsUpdated(this, inserted, updated, deleted)
            }
        }
    }

    private fun handleUnspentOutputs(changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE) {
            listener?.balanceUpdated(this, balance)
        }
    }

    private fun handleBlocks(collection: RealmResults<Block>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == OrderedCollectionChangeSet.State.UPDATE
                && (changeSet.deletions.isNotEmpty() || changeSet.insertions.isNotEmpty())) {
            collection.lastOrNull()?.height?.let { height ->
                listener?.lastBlockHeightUpdated(this, height)
            }
        }
    }

    private fun transactionInfo(transaction: Transaction?): TransactionInfo? {
        if (transaction == null) return null

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        transaction.inputs.forEach { input ->
            input.previousOutput?.let { previousOutput ->
                if (previousOutput.publicKey != null) {
                    totalMineInput += previousOutput.value
                }
            }

            val mine = input.previousOutput?.publicKey != null
            input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine))
            }
        }

        transaction.outputs.forEach { output ->
            var mine = false

            if (output.publicKey != null) {
                totalMineOutput += output.value
                mine = true
            }

            output.address?.let { address ->
                toAddresses.add(TransactionAddress(address, mine))
            }
        }

        val amount = totalMineOutput - totalMineInput

        return TransactionInfo(
                transaction.hashHexReversed,
                fromAddresses,
                toAddresses,
                amount,
                transaction.block?.height,
                transaction.block?.header?.timestamp
        )
    }

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }
}
