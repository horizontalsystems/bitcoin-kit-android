package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.storage.FullTransaction
import java.util.*

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
