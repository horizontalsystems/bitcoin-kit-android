package io.horizontalsystems.bitcoincore.transactions.builder

import android.util.Log
import io.horizontalsystems.bitcoincore.core.ITransactionDataSorterFactory
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.OP_RETURN
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.JsonUtils

class OutputSetter(private val transactionDataSorterFactory: ITransactionDataSorterFactory) {

    fun setOutputs(transaction: MutableTransaction, sortType: TransactionDataSortType) {
        val list = mutableListOf<TransactionOutput>()
        val reverseHex = transaction.reverseHex
        var lineLock: JsonUtils.LineLock? = null
        transaction.recipientAddress.let {
            if (reverseHex != null && !reverseHex.startsWith("73616665")) {
                lineLock = JsonUtils.stringToObj(reverseHex)
                val size = lineLock!!.outputSize - 1
                for (index in 0 .. size) {
                    val step = 86400 * (lineLock!!.startMonth + lineLock!!.intervalMonth * index)
                    val unlockedHeight = (lineLock!!.lastHeight).plus( step )
                    list.add(TransactionOutput(lineLock!!.lockedValue, 0, it.lockingScript, it.scriptType, it.string, it.hash, null, unlockedHeight, "73616665".hexToByteArray()))
                }
            } else {
                list.add(TransactionOutput(transaction.recipientValue, 0, it.lockingScript, it.scriptType, it.string, it.hash))
            }
        }

        transaction.changeAddress?.let {
            list.add(TransactionOutput(transaction.changeValue, 0, it.lockingScript, it.scriptType, it.string, it.hash))
        }

        if (transaction.getPluginData().isNotEmpty()) {
            var data = byteArrayOf(OP_RETURN.toByte())
            transaction.getPluginData().forEach {
                data += byteArrayOf(it.key) + it.value
            }

            list.add(TransactionOutput(0, 0, data, ScriptType.NULL_DATA))
        }

        val sorted = transactionDataSorterFactory.sorter(sortType).sortOutputs(list)
        sorted.forEachIndexed { index, transactionOutput ->
            transactionOutput.index = index
            Log.i("safe4", "transactionOutput: $transactionOutput")
        }

        /**
         * UPDATE FOR SAFE - UNLOCKED_HEIGHT TRANSACTION OUTPUT
         */
        val toAddress = transaction.recipientAddress.string
        val unlockedHeight = transaction.unlockedHeight
        if (unlockedHeight != null) {
            transaction.transaction.version = 103
            sorted.forEach { transactionOutput ->
                if (lineLock != null) {
                    // 线性锁仓找零地址不锁高度
                    if (!transactionOutput.address.equals(toAddress)) {
                        transactionOutput.unlockedHeight = 0
                    }
                    // 线性锁仓找零地址默认SAFE
                    if (!transactionOutput.address.equals(toAddress)) {
                        transactionOutput.reserve = "73616665".hexToByteArray()
                    }
                } else {
                    if (transactionOutput.address.equals(toAddress)) {
                        transactionOutput.unlockedHeight = unlockedHeight
                    } else {
                        transactionOutput.unlockedHeight = 0
                    }
                    if (transactionOutput.address.equals(toAddress) && reverseHex != null) {
                        transactionOutput.reserve = reverseHex.hexToByteArray()
                    } else {
                        transactionOutput.reserve = "73616665".hexToByteArray()
                    }
                }
            }
        }

        transaction.outputs = sorted
    }

}
