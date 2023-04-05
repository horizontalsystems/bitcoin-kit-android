package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
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

val Purpose.scriptType: ScriptType
    get() = when (this) {
        Purpose.BIP44 -> ScriptType.P2PKH
        Purpose.BIP49 -> ScriptType.P2WPKHSH
        Purpose.BIP84 -> ScriptType.P2WPKH
        Purpose.BIP86 -> ScriptType.P2TR
    }

val Purpose.description: String
    get() = when (this) {
        Purpose.BIP44 -> "bip44"
        Purpose.BIP49 -> "bip49"
        Purpose.BIP84 -> "bip84"
        Purpose.BIP86 -> "bip86"
    }
