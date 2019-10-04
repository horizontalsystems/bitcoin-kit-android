package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign

class MutableTransaction {
    lateinit var paymentOutput: TransactionOutput

    val outputs: List<TransactionOutput>
        get() {
            val list = mutableListOf(paymentOutput)
            changeOutput?.let {
                list.add(it)
            }
            return list
        }

    val transaction = Transaction(1, 0)
    var changeOutput: TransactionOutput? = null

    val inputsToSign = mutableListOf<InputToSign>()

    init {
        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = true
    }

    fun addInput(inputToSign: InputToSign) {
        inputsToSign.add(inputToSign)
    }

    fun build(): FullTransaction {
        return FullTransaction(transaction, inputsToSign.map { it.input }, outputs)
    }

}
