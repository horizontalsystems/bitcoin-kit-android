package io.horizontalsystems.bitcoinkit

import android.content.Context
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.blocks.Blockchain
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.exceptions.AddressFormatException
import io.horizontalsystems.bitcoinkit.managers.*
import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.transactions.TransactionCreator
import io.horizontalsystems.bitcoinkit.transactions.TransactionExtractor
import io.horizontalsystems.bitcoinkit.transactions.TransactionLinker
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.OrderedCollectionChangeSet
import io.realm.Realm
import io.realm.RealmResults
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class WalletKitModule

class WalletKit(words: List<String>, networkType: NetworkType) {

    interface Listener {
        fun transactionsUpdated(walletKit: WalletKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun balanceUpdated(walletKit: WalletKit, balance: Long)
        fun lastBlockInfoUpdated(walletKit: WalletKit, lastBlockInfo: BlockInfo)
        fun progressUpdated(walletKit: WalletKit, progress: Double)
    }

    enum class NetworkType { MainNet, TestNet, RegTest, MainNetBitCash, TestNetBitCash }

    var listener: Listener? = null

    val transactions: List<TransactionInfo>
        get() = transactionRealmResults.mapNotNull { transactionInfo(it) }

    val lastBlockHeight: Int
        get() = blockRealmResults.lastOrNull()?.height ?: 0

    val balance: Long
        get() = transactionOutputRealmResults.filter { it.inputs?.size ?: 0 == 0 }.map { it.value }.sum()

    private val initialSyncer: InitialSyncer
    private val addressManager: AddressManager
    private val transactionCreator: TransactionCreator
    private val transactionBuilder: TransactionBuilder

    private val transactionRealmResults: RealmResults<Transaction>

    // we use transactionOutputRealmResults instead of unspentOutputRealmResults
    // since Realm java does not support Collection aggregate queries as in Swift
    private val transactionOutputRealmResults: RealmResults<TransactionOutput>
    private val blockRealmResults: RealmResults<Block>

    private var peerGroup: PeerGroup

    private var realmFactory: RealmFactory

    private val network: NetworkParameters

    private var addressConverter: AddressConverter

    init {
        realmFactory = RealmFactory(networkType.name)
        val realm = realmFactory.realm

        network = when (networkType) {
            NetworkType.MainNet -> MainNet()
            NetworkType.MainNetBitCash -> MainNetBitcoinCash()
            NetworkType.TestNet -> TestNet()
            NetworkType.TestNetBitCash -> TestNetBitcoinCash()
            NetworkType.RegTest -> RegTest()
        }

        val wallet = HDWallet(Mnemonic().toSeed(words), network.coinType)
        val peerManager = PeerManager(network)

        val pubKeys = realm.where(PublicKey::class.java).findAll()
        val bloomFilterManager = BloomFilterManager(pubKeys.map { it.publicKey }, realmFactory)

        addressConverter = AddressConverter(network)
        addressManager = AddressManager(realmFactory, wallet, addressConverter)

        val transactionExtractor = TransactionExtractor(addressConverter)
        val transactionLinker = TransactionLinker()

        val transactionProcessor = TransactionProcessor(transactionExtractor, transactionLinker)
        val addressConverter = AddressConverter(network)

        peerGroup = PeerGroup(peerManager, bloomFilterManager, network, 1)
        peerGroup.blockSyncer = BlockSyncer(realmFactory, Blockchain(network), transactionProcessor, addressManager, bloomFilterManager, network)


        val apiManager = ApiManager("http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/blockstore")
        val stateManager = StateManager(realmFactory)

        val blockDiscover = BlockDiscover(wallet, apiManager, network, addressConverter)

        initialSyncer = InitialSyncer(realmFactory, blockDiscover, stateManager, addressManager, peerGroup)

        transactionBuilder = TransactionBuilder(realmFactory, addressConverter, wallet)
        transactionCreator = TransactionCreator(realmFactory, transactionBuilder, transactionProcessor, peerGroup, addressManager)

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

    fun send(address: String, value: Int) {
        transactionCreator.create(address, value)
    }

    fun fee(value: Int, address: String? = null, senderPay: Boolean = true) {
        transactionBuilder.fee(value, transactionCreator.feeRate, senderPay, address)
    }

    fun receiveAddress(): String {
        return addressManager.receiveAddress()
    }

    @Throws(AddressFormatException::class)
    fun validateAddress(address: String) {
        addressConverter.convert(address)
    }

    fun clear() {
        val realm = realmFactory.realm
        realm.executeTransaction {
            it.deleteAll()
        }
        realm.close()
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
            collection.lastOrNull()?.let { block ->
                listener?.lastBlockInfoUpdated(this,
                        BlockInfo(block.reversedHeaderHashHex, block.height, block.header?.timestamp))
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
