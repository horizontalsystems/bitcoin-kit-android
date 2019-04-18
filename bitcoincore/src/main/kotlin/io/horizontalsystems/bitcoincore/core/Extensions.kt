package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.hdwalletkit.HDWallet
import java.util.*

// https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

fun String.hexStringToByteArray(): ByteArray {
    return this.hexToByteArray()
}

fun HDWallet.publicKey(account: Int, index: Int, external: Boolean): PublicKey {
    val hdPubKey = hdPublicKey(account, index, external)
    return PublicKey(account, index, hdPubKey.external, hdPubKey.publicKey, hdPubKey.publicKeyHash)
}

fun List<FullTransaction>.inTopologicalOrder(): List<FullTransaction> {

    fun visit(v: Int, visited: MutableList<Boolean>, stack: Stack<FullTransaction>) {
        if (visited[v])
            return

        visited[v] = true
        val currentTx = this[v]

        for (i in 0 until this.size) {
            for (input in this[i].inputs) {
                if (input.previousOutputTxReversedHex == currentTx.header.hashHexReversed && input.previousOutputIndex < currentTx.outputs.size) {
                    visit(i, visited, stack)
                }
            }
        }

        stack.push(currentTx)
    }

    val stack = Stack<FullTransaction>()
    val visited = MutableList(this.size) { false }

    for (i in 0 until this.size) {
        if (!visited[i]) {
            visit(i, visited, stack)
        }
    }

    val ordered = mutableListOf<FullTransaction>()
    while (stack.isNotEmpty()) {
        ordered.add(stack.pop())
    }

    return ordered
}
