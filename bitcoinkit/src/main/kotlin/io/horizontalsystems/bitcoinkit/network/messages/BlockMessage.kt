package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.models.Transaction
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * The 'block' message consists of a single serialized block.
 */
class BlockMessage() : Message("block") {

    lateinit var header: Header
    lateinit var transactions: Array<Transaction>

    @Throws(IOException::class)
    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            header = Header(input)
            val txCount = input.readVarInt() // do not store count
            transactions = Array(txCount.toInt()) {
                Transaction(input)
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.write(header.toByteArray())
        output.writeVarInt(transactions.size.toLong())
        for (transaction in transactions) {
            output.write(transaction.toByteArray())
        }

        return output.toByteArray()
    }

    /**
     * Validate block hash.
     */
    fun validateHash(): Boolean {
        // TODO: validate bits:
        return true
    }

    override fun toString(): String {
        return "BlockMessage(txCount=${transactions.size})"
    }
}
