package io.horizontalsystems.bitcoincore.core

import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.JsonValue
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.hdwalletkit.HDWallet
import java.util.*

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
                if (input.previousOutputTxHash.contentEquals(currentTx.header.hash) && input.previousOutputIndex < currentTx.outputs.size) {
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

fun JsonObject.getOrMappingError(name: String): JsonValue {
    return get(name) ?: throw MappingError(name)
}
