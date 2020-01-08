package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

object Bip69 {

    private const val before = -1
    private const val equal = 0
    private const val after = 1

    val outputComparator = kotlin.Comparator<TransactionOutput> { o1, o2 ->
        //sort by amount first
        val valueCompareResult = o1.value.compareTo(o2.value)
        if (valueCompareResult != equal) {
            return@Comparator valueCompareResult
        }

        val keyHash1 = o1.keyHash ?: return@Comparator after
        val keyHash2 = o2.keyHash ?: return@Comparator before

        //when amounts are equal, sort by hash
        val hashCompareResult = compareByteArrays(keyHash1, keyHash2)
        if (hashCompareResult != equal) {
            return@Comparator hashCompareResult
        }

        //sort by hash size
        return@Comparator keyHash1.size.compareTo(keyHash2.size)
    }

    val inputComparator = kotlin.Comparator<UnspentOutput> { o1, o2 ->
        //sort by hash first
        val result = compareByteArrays(o1.output.transactionHash, o2.output.transactionHash)
        if (result != equal) {
            return@Comparator result
        }

        //sort by index
        return@Comparator o1.output.index.compareTo(o2.output.index)
    }

    private fun compareByteArrays(b1: ByteArray, b2: ByteArray): Int {
        var pos = 0

        while (pos < b1.size && pos < b2.size) {
            val result = (b1[pos].toInt() and 0xff).compareTo(b2[pos].toInt() and 0xff)
            if (result == equal){
                pos++
            } else {
                return result
            }
        }

        return equal
    }
}
