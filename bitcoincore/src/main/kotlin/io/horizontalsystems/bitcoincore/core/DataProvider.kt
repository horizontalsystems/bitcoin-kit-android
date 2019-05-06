package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.managers.UnspentOutputProvider
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo
import io.horizontalsystems.bitcoincore.storage.TransactionWithBlock
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

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?) {
        listener?.onTransactionsUpdate(
                storage.getFullTransactionInfo(inserted.map { TransactionWithBlock(it, block) }).mapNotNull { transactionInfo(it) },
                storage.getFullTransactionInfo(updated.map { TransactionWithBlock(it, block) }).mapNotNull { transactionInfo(it) }
        )

        balanceUpdateSubject.onNext(true)
    }

    override fun onTransactionsDelete(hashes: List<String>) {
        listener?.onTransactionsDelete(hashes)
        balanceUpdateSubject.onNext(true)
    }

    fun clear() {
        balanceSubjectDisposable.dispose()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> =
            Single.create { emitter ->
                var results = listOf<FullTransactionInfo>()
                if (fromHash != null) {
                    storage.getTransaction(fromHash.toReversedByteArray())?.let {
                        results = storage.getFullTransactionInfo(it, limit)
                    }
                }
                else {
                    results = storage.getFullTransactionInfo(null, limit)
                }

                emitter.onSuccess(results.mapNotNull { transactionInfo(it) })
            }

    private fun blockInfo(block: Block) = BlockInfo(
            block.headerHash.toReversedHex(),
            block.height,
            block.timestamp)

    private fun transactionInfo(fullTransaction: FullTransactionInfo): TransactionInfo? {
        val transaction = fullTransaction.header

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        fullTransaction.inputs.forEach { input ->
            var mine = false

            if (input.previousOutput?.publicKeyPath != null) {
                totalMineInput += input.previousOutput.value
                mine = true

            }

            input.input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine = mine))
            }
        }

        fullTransaction.outputs.forEach { output ->
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
                transactionHash = transaction.hash.toReversedHex(),
                transactionIndex = transaction.order,
                from = fromAddresses,
                to = toAddresses,
                amount = totalMineOutput - totalMineInput,
                blockHeight = fullTransaction.block?.height,
                timestamp = transaction.timestamp
        )
    }

}
