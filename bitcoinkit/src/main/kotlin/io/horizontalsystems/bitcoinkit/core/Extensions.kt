package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet
import java.util.*

// https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

fun HDWallet.publicKey(account: Int, index: Int, external: Boolean): PublicKey {
    val hdPubKey = hdPublicKey(account, index, external)
    return PublicKey(account, index, hdPubKey.external, hdPubKey.publicKey, hdPubKey.publicKeyHash)
}

fun List<Transaction>.inTopologicalOrder(): List<Transaction> {

    fun visit(v: Int, visited: MutableList<Boolean>, stack: Stack<Transaction>) {
        if (visited[v])
            return

        visited[v] = true
        val currentTx = this[v]

        for (i in 0 until this.size) {
            for (input in this[i].inputs) {
                if (input.previousOutputHexReversed == currentTx.hashHexReversed &&
                        input.previousOutputIndex < currentTx.outputs.size) {
                    visit(i, visited, stack)
                }
            }
        }

        stack.push(currentTx)
    }

    val stack = Stack<Transaction>()
    val visited = MutableList(this.size) { false }

    for (i in 0 until this.size) {
        if (!visited[i]) {
            visit(i, visited, stack)
        }
    }

    val ordered = mutableListOf<Transaction>()
    while (stack.isNotEmpty()) {
        ordered.add(stack.pop())
    }

    return ordered
}
