package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.realm.Sort
import java.util.concurrent.TimeUnit

class DataProvider(private val realmFactory: RealmFactory, private val listener: Listener, private val unspentOutputProvider: UnspentOutputProvider) : IBlockchainDataListener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>)
        fun onTransactionsDelete(hashes: List<String>)
        fun onBalanceUpdate(balance: Long)
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo)
    }

    private val balanceUpdateSubject: PublishSubject<Boolean> = PublishSubject.create()
    private val balanceSubjectDisposable: Disposable

    //  Getters
    var balance: Long = unspentOutputProvider.getBalance()
        private set

    var lastBlockInfo: BlockInfo?
        private set

    val feeRate: FeeRate
        get() = realmFactory.realm.use { realm ->
            realm.where(FeeRate::class.java).findAll().firstOrNull()?.let { realm.copyFromRealm(it) }
                    ?: FeeRate.defaultFeeRate
        }

    init {
        lastBlockInfo = realmFactory.realm.use { realm ->
            realm.where(Block::class.java)
                    .sort("height", Sort.DESCENDING)
                    .findFirst()?.let { blockInfo(it) }
        }

        balanceSubjectDisposable = balanceUpdateSubject.debounce(500, TimeUnit.MILLISECONDS)
//                .observeOn(AndroidSchedulers.from(Looper.myLooper()))
                .subscribe {
                    balance = unspentOutputProvider.getBalance()
                    listener.onBalanceUpdate(balance)
                }
    }

    override fun onBlockInsert(block: Block) {
        if (block.height > lastBlockInfo?.height ?: 0) {
            val blockInfo = blockInfo(block)

            lastBlockInfo = blockInfo
            listener.onLastBlockInfoUpdate(blockInfo)
            balanceUpdateSubject.onNext(true)
        }
    }

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>) {
        listener.onTransactionsUpdate(inserted.mapNotNull { transactionInfo(it) }, updated.mapNotNull { transactionInfo(it) })
        balanceUpdateSubject.onNext(true)
    }

    override fun onTransactionsDelete(ids: List<String>) {
        listener.onTransactionsDelete(ids)
        balanceUpdateSubject.onNext(true)
    }

    fun clear() {
        balanceSubjectDisposable.dispose()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> =
            Single.create { emitter ->
                realmFactory.realm.use { realm ->

                    var results = realm.where(Transaction::class.java)
                            .sort("timestamp", Sort.DESCENDING, "order", Sort.DESCENDING)

                    fromHash?.let { fromHash ->
                        realm.where(Transaction::class.java).equalTo("hashHexReversed", fromHash).findFirst()?.let { fromTransaction ->
                            results = results
                                    .beginGroup()
                                        .lessThan("timestamp", fromTransaction.timestamp)
                                        .or()
                                        .beginGroup()
                                            .equalTo("timestamp", fromTransaction.timestamp)
                                            .lessThan("order", fromTransaction.order)
                                        .endGroup()
                                    .endGroup()
                        }
                    }

                    limit?.let {
                        results = results.limit(it.toLong())
                    }

                    emitter.onSuccess(results.findAll().mapNotNull { transactionInfo(it) })
                }
            }

    private fun blockInfo(block: Block) = BlockInfo(
            block.reversedHeaderHashHex,
            block.height,
            block.header?.timestamp)

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

            input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine = input.previousOutput?.publicKey != null))
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

        return TransactionInfo(
                transactionHash = transaction.hashHexReversed,
                from = fromAddresses,
                to = toAddresses,
                amount = totalMineOutput - totalMineInput,
                blockHeight = transaction.block?.height,
                timestamp = transaction.timestamp
        )
    }

}
