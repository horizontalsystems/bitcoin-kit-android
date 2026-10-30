package io.horizontalsystems.litecoinmwebkit

import java.io.ByteArrayOutputStream

/** An MWEB input of a template: the output ID of the coin and the index of the address it was received on. */
class TemplateInput(val outputId: ByteArray, val addressIndex: Int) {
    init {
        require(outputId.size == 32) { "output id must be 32 bytes" }
    }
}

/** An output of a template or of a Litecoin transaction: script and value in litoshi. */
class TemplateOutput(val script: ByteArray, val value: Long)

class RawInput(val previousHash: ByteArray, val previousIndex: Long, val script: ByteArray, val sequence: Long)

/**
 * A Litecoin transaction split into its parts. The MWEB extension (flag bit
 * 0x08) is carried as opaque bytes: the daemon produces and consumes it, the
 * Kotlin side only needs to edit the canonical inputs and outputs around it.
 */
class RawTransaction(
    val version: Int,
    val inputs: MutableList<RawInput>,
    val outputs: MutableList<TemplateOutput>,
    val witnesses: MutableList<List<ByteArray>>?,
    val mweb: ByteArray?,
    val lockTime: Long,
)

/**
 * Serializes transaction templates for the daemon's `Create` RPC and parses
 * the transactions it returns.
 *
 * In a template, MWEB inputs are inputs whose outpoint hash is the output ID
 * and whose outpoint index is the address index; MWEB outputs carry the
 * 66-byte scan+spend key script of the destination address.
 */
object MwebTxSerializer {
    private const val MARKER = 0x00
    private const val WITNESS_FLAG = 0x01
    private const val MWEB_FLAG = 0x08

    fun serializeTemplate(inputs: List<TemplateInput>, outputs: List<TemplateOutput>): ByteArray {
        val raw = RawTransaction(
            version = 2,
            inputs = inputs.map { RawInput(it.outputId, it.addressIndex.toLong(), byteArrayOf(), 0xffffffffL) }.toMutableList(),
            outputs = outputs.toMutableList(),
            witnesses = null,
            mweb = null,
            lockTime = 0,
        )
        return serialize(raw)
    }

    fun serialize(tx: RawTransaction, includeWitness: Boolean = true, includeMweb: Boolean = true): ByteArray {
        val out = ByteArrayOutputStream()
        writeInt32(out, tx.version)
        val witnesses = tx.witnesses?.takeIf { includeWitness && it.any { w -> w.isNotEmpty() } }
        val mweb = tx.mweb?.takeIf { includeMweb && it.isNotEmpty() }
        if (witnesses != null || mweb != null) {
            out.write(MARKER)
            out.write((if (witnesses != null) WITNESS_FLAG else 0) or (if (mweb != null) MWEB_FLAG else 0))
        }
        writeVarInt(out, tx.inputs.size.toLong())
        for (input in tx.inputs) {
            out.write(input.previousHash)
            writeInt32(out, input.previousIndex.toInt())
            writeVarInt(out, input.script.size.toLong())
            out.write(input.script)
            writeInt32(out, input.sequence.toInt())
        }
        writeVarInt(out, tx.outputs.size.toLong())
        for (output in tx.outputs) {
            writeInt64(out, output.value)
            writeVarInt(out, output.script.size.toLong())
            out.write(output.script)
        }
        if (witnesses != null) {
            for (i in tx.inputs.indices) {
                val items = witnesses.getOrElse(i) { emptyList() }
                writeVarInt(out, items.size.toLong())
                for (item in items) {
                    writeVarInt(out, item.size.toLong())
                    out.write(item)
                }
            }
        }
        if (mweb != null) {
            out.write(mweb)
        }
        writeInt32(out, tx.lockTime.toInt())
        return out.toByteArray()
    }

    fun parse(bytes: ByteArray): RawTransaction {
        val reader = Reader(bytes)
        val version = reader.int32()
        var inputCount = reader.varInt()
        var flags = 0
        if (inputCount == 0L && reader.remaining() > 0) {
            flags = reader.byte()
            if (flags == 0) throw IllegalArgumentException("Invalid transaction flags")
            inputCount = reader.varInt()
        }
        val inputs = ArrayList<RawInput>(inputCount.toInt())
        for (i in 0 until inputCount) {
            val hash = reader.bytes(32)
            val index = reader.int32().toLong() and 0xffffffffL
            val script = reader.bytes(reader.varInt().toInt())
            val sequence = reader.int32().toLong() and 0xffffffffL
            inputs.add(RawInput(hash, index, script, sequence))
        }
        val outputCount = reader.varInt()
        val outputs = ArrayList<TemplateOutput>(outputCount.toInt())
        for (i in 0 until outputCount) {
            val value = reader.int64()
            val script = reader.bytes(reader.varInt().toInt())
            outputs.add(TemplateOutput(script, value))
        }
        var witnesses: MutableList<List<ByteArray>>? = null
        if (flags and WITNESS_FLAG != 0) {
            witnesses = ArrayList(inputs.size)
            for (i in inputs.indices) {
                val count = reader.varInt().toInt()
                witnesses.add(List(count) { reader.bytes(reader.varInt().toInt()) })
            }
        }
        var mweb: ByteArray? = null
        if (flags and MWEB_FLAG != 0) {
            // Everything up to the trailing lock time is the MWEB extension.
            if (reader.remaining() < 4) throw IllegalArgumentException("Truncated transaction")
            mweb = reader.bytes(reader.remaining() - 4)
        }
        val lockTime = reader.int32().toLong() and 0xffffffffL
        if (reader.remaining() != 0) throw IllegalArgumentException("Trailing bytes after transaction")
        return RawTransaction(version, inputs, outputs, witnesses, mweb, lockTime)
    }

    fun varIntSize(value: Long): Int = when {
        value < 0xfd -> 1
        value <= 0xffff -> 3
        value <= 0xffffffffL -> 5
        else -> 9
    }

    private fun writeVarInt(out: ByteArrayOutputStream, value: Long) {
        when {
            value < 0xfd -> out.write(value.toInt())
            value <= 0xffff -> {
                out.write(0xfd); out.write(value.toInt() and 0xff); out.write((value.toInt() ushr 8) and 0xff)
            }
            value <= 0xffffffffL -> {
                out.write(0xfe); writeInt32(out, value.toInt())
            }
            else -> {
                out.write(0xff); writeInt64(out, value)
            }
        }
    }

    private fun writeInt32(out: ByteArrayOutputStream, value: Int) {
        for (i in 0 until 4) out.write((value ushr (8 * i)) and 0xff)
    }

    private fun writeInt64(out: ByteArrayOutputStream, value: Long) {
        for (i in 0 until 8) out.write(((value ushr (8 * i)) and 0xff).toInt())
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
