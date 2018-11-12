package io.horizontalsystems.bitcoinkit

import android.content.Context
import android.os.Handler
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.blocks.Blockchain
import io.horizontalsystems.bitcoinkit.blocks.ProgressSyncer
import io.horizontalsystems.bitcoinkit.core.DataProvider
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.*
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.network.*
import io.horizontalsystems.bitcoinkit.transactions.*
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.Realm
import io.realm.annotations.RealmModule

@RealmModule(library = true, allClasses = true)
class BitcoinKitModule

class BitcoinKit(words: List<String>, networkType: NetworkType) : ProgressSyncer.Listener, DataProvider.Listener {

    interface Listener {
        fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long)
        fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo)
        fun onProgressUpdate(bitcoinKit: BitcoinKit, progress: Double)
    }

    var listener: Listener? = null

    //  DataProvider getters
    val balance get() = dataProvider.balance
    val transactions get() = dataProvider.transactions
    val lastBlockHeight get() = dataProvider.lastBlockHeight

    private val peerGroup: PeerGroup
    private val initialSyncer: InitialSyncer
    private val addressManager: AddressManager
    private val addressConverter: AddressConverter
    private val transactionCreator: TransactionCreator
    private val transactionBuilder: TransactionBuilder
    private val dataProvider: DataProvider
    private val realmFactory = RealmFactory(networkType.name)

    private val handler = Handler()
    private val network = when (networkType) {
        NetworkType.MainNet -> MainNet()
        NetworkType.MainNetBitCash -> MainNetBitcoinCash()
        NetworkType.TestNet -> TestNet()
        NetworkType.TestNetBitCash -> TestNetBitcoinCash()
        NetworkType.RegTest -> RegTest()
    }

    init {
        val realm = realmFactory.realm
        val wallet = HDWallet(Mnemonic().toSeed(words), network.coinType)

        dataProvider = DataProvider(realm, this)
        addressConverter = AddressConverter(network)
        addressManager = AddressManager(realmFactory, wallet, addressConverter)

        val peerManager = PeerManager(network, realmFactory)
        val progressSyncer = ProgressSyncer(this)
        val transactionLinker = TransactionLinker()
        val transactionExtractor = TransactionExtractor(addressConverter)
        val transactionProcessor = TransactionProcessor(transactionExtractor, transactionLinker, addressManager)
        val bloomFilterManager = BloomFilterManager(realmFactory)

        peerGroup = PeerGroup(peerManager, bloomFilterManager, network, 1)
        peerGroup.blockSyncer = BlockSyncer(realmFactory, Blockchain(network), transactionProcessor, addressManager, bloomFilterManager, progressSyncer, network)
        peerGroup.transactionSyncer = TransactionSyncer(realmFactory, transactionProcessor, addressManager, bloomFilterManager)
        peerGroup.lastBlockHeightListener = progressSyncer

        val addressSelector = when (networkType) {
            NetworkType.MainNet,
            NetworkType.TestNet,
            NetworkType.RegTest -> BitcoinAddressSelector(addressConverter)
            NetworkType.MainNetBitCash,
            NetworkType.TestNetBitCash -> BitcoinCashAddressSelector(addressConverter)
        }

        val apiManager = ApiManagerBtcCom(ApiRequesterBtcCom(networkType), addressSelector)
        val stateManager = StateManager(realmFactory)
        val blockDiscover = BlockDiscover(wallet, apiManager, network)

        initialSyncer = InitialSyncer(realmFactory, blockDiscover, stateManager, addressManager, peerGroup)
        transactionBuilder = TransactionBuilder(realmFactory, addressConverter, wallet)
        transactionCreator = TransactionCreator(realmFactory, transactionBuilder, transactionProcessor, peerGroup, addressManager)
    }

    //
    // API methods
    //
    fun start() {
        initialSyncer.sync()
    }

    fun send(address: String, value: Int) {
        transactionCreator.create(address, value)
    }

    fun receiveAddress(): String {
        return addressManager.receiveAddress()
    }

    fun validateAddress(address: String) {
        addressConverter.convert(address)
    }

    fun fee(value: Int, address: String? = null, senderPay: Boolean = true): Int {
        return transactionBuilder.fee(value, transactionCreator.feeRate, senderPay, address)
    }

    fun clear() = realmFactory.realm.use { realm ->
        realm.executeTransaction { realm.deleteAll() }
    }

    //
    // DataProvider Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        listener?.onTransactionsUpdate(this, inserted, updated, deleted)
    }

    override fun onBalanceUpdate(balance: Long) {
        listener?.onBalanceUpdate(this, balance)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        listener?.onLastBlockInfoUpdate(this, blockInfo)
    }

    //
    // ProgressSyncer Listener implementations
    //
    override fun onProgressUpdate(progress: Double) {
        handler.post { listener?.onProgressUpdate(this, progress) }
    }

    enum class NetworkType {
        MainNet,
        TestNet,
        RegTest,
        MainNetBitCash,
        TestNetBitCash
    }

    companion object {
        fun init(context: Context) {
            Realm.init(context)
        }
    }
}
