package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.serializers.BlockHeaderSerializer
import io.horizontalsystems.bitcoinkit.serializers.TransactionSerializer
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * The 'block' message consists of a single serialized block.
 */
class BlockMessage() : Message("block") {

    lateinit var header: BlockHeader
    lateinit var transactions: Array<FullTransaction>

    @Throws(IOException::class)
    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            header = BlockHeaderSerializer.deserialize(input)
            val txCount = input.readVarInt() // do not store count
            transactions = Array(txCount.toInt()) {
                TransactionSerializer.deserialize(input)
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.write(BlockHeaderSerializer.serialize(header))
        output.writeVarInt(transactions.size.toLong())
        for (transaction in transactions) {
            output.write(TransactionSerializer.serialize(transaction))
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
