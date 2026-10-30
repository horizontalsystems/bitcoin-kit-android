package io.horizontalsystems.bitcoincore.serializers

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import java.io.ByteArrayOutputStream

/**
 * A transaction taken apart by [RawTransactionCodec]. [extension] holds the
 * bytes a fork places between the witness data and the lock time when it
 * sets a flag bit other than the witness bit (Litecoin's MWEB extension,
 * flag 0x08). The codec does not interpret them and writes them back as
 * they were.
 */
class ParsedRawTransaction(
    val version: Int,
    val inputs: List<TransactionInput>,
    val outputs: List<TransactionOutput>,
    val extensionFlags: Int,
    val extension: ByteArray?,
    val lockTime: Long,
)

/**
 * Parses and serializes transactions whose inputs may be unsigned and
 * whose serialization may carry a fork-specific extension. The witness
 * marker/flag convention is BIP144's: a zero input count followed by a flag
 * byte, bit 0x01 for witness data and any other bit for an extension that
 * sits between the witness data and the lock time.
 */
object RawTransactionCodec {
    private const val WITNESS_FLAG = 0x01

    fun parse(bytes: ByteArray): ParsedRawTransaction {
        val reader = Reader(bytes)
        val version = reader.int32()
        var inputCount = reader.varInt()
        var flags = 0
        if (inputCount == 0L && reader.remaining() > 0) {
            flags = reader.byte()
            if (flags == 0) throw IllegalArgumentException("Invalid transaction flags")
            inputCount = reader.varInt()
        }
        val inputs = ArrayList<TransactionInput>(inputCount.toInt())
        for (i in 0 until inputCount) {
            val hash = reader.bytes(32)
            val index = reader.int32().toLong() and 0xffffffffL
            val script = reader.bytes(reader.varInt().toInt())
            val sequence = reader.int32().toLong() and 0xffffffffL
            inputs.add(TransactionInput(hash, index, script, sequence))
        }
        val outputCount = reader.varInt()
        val outputs = ArrayList<TransactionOutput>(outputCount.toInt())
        for (i in 0 until outputCount) {
            val value = reader.int64()
            val script = reader.bytes(reader.varInt().toInt())
            outputs.add(TransactionOutput(value, i.toInt(), script))
        }
        if (flags and WITNESS_FLAG != 0) {
            for (input in inputs) {
                val count = reader.varInt().toInt()
                input.witness = List(count) { reader.bytes(reader.varInt().toInt()) }
            }
        }
        val extensionFlags = flags and WITNESS_FLAG.inv()
        var extension: ByteArray? = null
        if (extensionFlags != 0) {
            if (reader.remaining() < 4) throw IllegalArgumentException("Truncated transaction")
            extension = reader.bytes(reader.remaining() - 4)
        }
        val lockTime = reader.int32().toLong() and 0xffffffffL
        if (reader.remaining() != 0) throw IllegalArgumentException("Trailing bytes after transaction")
        return ParsedRawTransaction(version, inputs, outputs, extensionFlags, extension, lockTime)
    }

    /**
     * Serializes [tx]. Witness data is written when any input has some and
     * [withWitness] is set; the extension is written when present and
     * [withExtension] is set.
     */
    fun serialize(tx: ParsedRawTransaction, withWitness: Boolean = true, withExtension: Boolean = true): ByteArray {
        val hasWitness = withWitness && tx.inputs.any { it.witness.isNotEmpty() }
        val extension = tx.extension?.takeIf { withExtension && tx.extensionFlags != 0 }
        val out = ByteArrayOutputStream()
        out.write(BitcoinOutput().writeInt(tx.version).toByteArray())
        if (hasWitness || extension != null) {
            out.write(0)
            out.write((if (hasWitness) WITNESS_FLAG else 0) or (if (extension != null) tx.extensionFlags else 0))
        }
        out.write(BitcoinOutput().writeVarInt(tx.inputs.size.toLong()).toByteArray())
        for (input in tx.inputs) {
            out.write(InputSerializer.serialize(input))
        }
        out.write(BitcoinOutput().writeVarInt(tx.outputs.size.toLong()).toByteArray())
        for (output in tx.outputs) {
            out.write(OutputSerializer.serialize(output))
        }
        if (hasWitness) {
            for (input in tx.inputs) {
                out.write(InputSerializer.serializeWitness(input.witness))
            }
        }
        if (extension != null) {
            out.write(extension)
        }
        out.write(BitcoinOutput().writeUnsignedInt(tx.lockTime).toByteArray())
        return out.toByteArray()
    }

    private class Reader(private val bytes: ByteArray) {
        private var pos = 0

        fun remaining() = bytes.size - pos

        fun byte(): Int {
            if (pos >= bytes.size) throw IllegalArgumentException("Truncated transaction")
            return bytes[pos++].toInt() and 0xff
        }

        fun bytes(n: Int): ByteArray {
            if (n < 0 || pos + n > bytes.size) throw IllegalArgumentException("Truncated transaction")
            val out = bytes.copyOfRange(pos, pos + n)
            pos += n
            return out
        }

        fun int32(): Int {
            var v = 0
            for (i in 0 until 4) v = v or (byte() shl (8 * i))
            return v
        }

        fun int64(): Long {
            var v = 0L
            for (i in 0 until 8) v = v or (byte().toLong() shl (8 * i))
            return v
        }

        fun varInt(): Long = when (val first = byte()) {
            0xfd -> (byte() or (byte() shl 8)).toLong()
            0xfe -> int32().toLong() and 0xffffffffL
            0xff -> int64()
            else -> first.toLong()
        }
    }
}
