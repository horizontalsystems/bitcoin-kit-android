package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoinkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class DataProvider(private val storage: IStorage, private val unspentOutputProvider: UnspentOutputProvider)
    : IBlockchainDataListener {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>)
        fun onTransactionsDelete(hashes: List<String>)
        fun onBalanceUpdate(balance: Long)
        fun onLastBlockInfoUpdate(blockInfo: BlockInfo)
    }

    var listener: Listener? = null
    private val balanceUpdateSubject: PublishSubject<Boolean> = PublishSubject.create()
    private val balanceSubjectDisposable: Disposable

    //  Getters
    var balance: Long = unspentOutputProvider.getBalance()
        private set(value) {
            if (value != field) {
                field = value
                listener?.onBalanceUpdate(field)
            }
        }

    var lastBlockInfo: BlockInfo?
        private set

    val feeRate: FeeRate
        get() = storage.feeRate ?: FeeRate.defaultFeeRate

    init {
        lastBlockInfo = storage.lastBlock()?.let {
            blockInfo(it)
        }

        balanceSubjectDisposable = balanceUpdateSubject.debounce(500, TimeUnit.MILLISECONDS)
                .subscribe {
                    balance = unspentOutputProvider.getBalance()
                }
    }

    override fun onBlockInsert(block: Block) {
        if (block.height > lastBlockInfo?.height ?: 0) {
            val blockInfo = blockInfo(block)

            lastBlockInfo = blockInfo
            listener?.onLastBlockInfoUpdate(blockInfo)
            balanceUpdateSubject.onNext(true)
        }
    }

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>) {
        listener?.onTransactionsUpdate(inserted.mapNotNull { transactionInfo(it) }, updated.mapNotNull { transactionInfo(it) })
        balanceUpdateSubject.onNext(true)
    }

    override fun onTransactionsDelete(ids: List<String>) {
        listener?.onTransactionsDelete(ids)
        balanceUpdateSubject.onNext(true)
    }

    fun clear() {
        balanceSubjectDisposable.dispose()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> =
            Single.create { emitter ->
                var results = storage.getTransactionsSortedTimestampAndOrdered()

                fromHash?.let { fromHash ->
                    storage.getTransaction(fromHash)?.let { fromTransaction ->
                        results.filter {
                            it.timestamp < fromTransaction.timestamp || (it.timestamp == fromTransaction.timestamp && it.order < fromTransaction.order)
                        }
                    }
                }

                limit?.let {
                    results = results.take(limit)
                }

                emitter.onSuccess(results.mapNotNull { transactionInfo(it) })
            }

    private fun blockInfo(block: Block) = BlockInfo(
            block.headerHashReversedHex,
            block.height,
            block.timestamp)

    private fun transactionInfo(transaction: Transaction?): TransactionInfo? {
        if (transaction == null) return null

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        storage.getTransactionInputs(transaction).forEach { input ->
            var mine = false

            val previousOutput = storage.getPreviousOutput(input)
            if (previousOutput?.publicKeyPath != null) {
                totalMineInput += previousOutput.value
                mine = true

            }

            input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine = mine))
            }
        }

        storage.getTransactionOutputs(transaction).forEach { output ->
            var mine = false

            if (output.publicKeyPath != null) {
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
                blockHeight = transaction.block(storage)?.height,
                timestamp = transaction.timestamp
        )
    }

}
