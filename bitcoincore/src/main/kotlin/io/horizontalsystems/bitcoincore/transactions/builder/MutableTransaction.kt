package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign

class MutableTransaction {
    lateinit var paymentOutput: TransactionOutput
    var changeOutput: TransactionOutput? = null
    var dataOutput: TransactionOutput? = null

    val outputs: List<TransactionOutput>
        get() {
            val list = mutableListOf(paymentOutput)
            changeOutput?.let {
                list.add(it)
            }
            dataOutput?.let {
                list.add(it)
            }
            return list
        }

    val inputsToSign = mutableListOf<InputToSign>()
    val transaction = Transaction(1, 0)

    init {
        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = true
    }

    fun getExtraDataOutputSize() : Long {
        return 0
    }

    fun addInput(inputToSign: InputToSign) {
        inputsToSign.add(inputToSign)
    }

    fun build(): FullTransaction {
        return FullTransaction(transaction, inputsToSign.map { it.input }, outputs)
    }

}
