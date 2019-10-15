package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.OP_RETURN
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

class MutableTransaction(isOutgoing: Boolean = true) {

    val inputsToSign = mutableListOf<InputToSign>()
    val transaction = Transaction(1, 0)

    lateinit var recipientAddress: Address
    var recipientValue = 0L

    var changeAddress: Address? = null
    var changeValue = 0L

    private val extraData = mutableMapOf<Int, ByteArray>()

    val outputs: List<TransactionOutput>
        get() {
            val list = mutableListOf<TransactionOutput>()

            var index = 0

            recipientAddress.let {
                list.add(TransactionOutput(recipientValue, index++, it.lockingScript, it.scriptType, it.string, it.hash))
            }

            changeAddress?.let {
                list.add(TransactionOutput(changeValue, index++, it.lockingScript, it.scriptType, it.string, it.hash))
            }

            if (extraData.isNotEmpty()) {
                var data = byteArrayOf(OP_RETURN.toByte())
                extraData.forEach {
                    data += byteArrayOf(it.key.toByte()) + it.value
                }

                list.add(TransactionOutput(0, index++, data, ScriptType.NULL_DATA))
            }

            return list
        }

    init {
        transaction.status = Transaction.Status.NEW
        transaction.isMine = true
        transaction.isOutgoing = isOutgoing
    }

    fun getPluginDataOutputSize(): Int {
        return if (extraData.isNotEmpty()) {
            1 + extraData.map { 1 + it.value.size }.sum()
        } else {
            0
        }
    }

    fun addInput(inputToSign: InputToSign) {
        inputsToSign.add(inputToSign)
    }

    fun addExtraData(id: Int, data: ByteArray) {
        extraData[id] = data
    }

    fun build(): FullTransaction {
        return FullTransaction(transaction, inputsToSign.map { it.input }, outputs)
    }

}
