package io.horizontalsystems.bitcoincore.transactions.scripts

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.utils.Utils
import java.io.ByteArrayInputStream

enum class ScriptType(val value: Int) {
    P2PKH(1),     // pay to pubkey hash (aka pay to address)
    P2PK(2),      // pay to pubkey
    P2SH(3),      // pay to script hash
    P2WPKH(4),    // pay to witness pubkey hash
    P2WSH(5),     // pay to witness script hash
    P2WPKHSH(6),  // P2WPKH nested in P2SH
    NULL_DATA(7),
    UNKNOWN(0);

    companion object {
        fun fromValue(value: Int): ScriptType? {
            return values().find { it.value == value }
        }
    }

    val isWitness: Boolean
        get() = this in arrayOf(P2WPKH, P2WSH, P2WPKHSH)
}

class Script(bytes: ByteArray) {
    val chunks = try {
        parseChunks(bytes)
    } catch (e: Exception) {
        listOf<Chunk>()
    }

    private fun parseChunks(bytes: ByteArray): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        val stream = ByteArrayInputStream(bytes)

        while (stream.available() > 0) {
            var dataToRead: Long = -1

            val opcode = stream.read()
            when (opcode) {
                in 0 until OP_PUSHDATA1 -> {
                    // Read some bytes of data, where how many is the opcode value itself.
                    dataToRead = opcode.toLong()
                }
                OP_PUSHDATA1 -> {
                    if (stream.available() < 1) throw Exception("Unexpected end of script")

                    dataToRead = stream.read().toLong()
                }
                OP_PUSHDATA2 -> {
                    // Read a short, then read that many bytes of data.
                    if (stream.available() < 2) throw Exception("Unexpected end of script")

                    dataToRead = Utils.readUint16FromStream(stream).toLong()
                }
                OP_PUSHDATA4 -> {
                    // Read a uint32, then read that many bytes of data.
                    // Though this is allowed, because its value cannot be > 520, it should never actually be used
                    if (stream.available() < 4) throw Exception("Unexpected end of script")

                    dataToRead = Utils.readUint32FromStream(stream)
                }
            }

            val chunk = when {
                dataToRead < 0 -> {
                    Chunk(opcode)
                }
                dataToRead > stream.available() -> {
                    throw Exception("Push of data element that is larger than remaining data")
                }
                else -> {
                    val data = ByteArray(dataToRead.toInt())
                    check(dataToRead == 0L || stream.read(data, 0, dataToRead.toInt()).toLong() == dataToRead)
                    Chunk(opcode, data)
                }
            }

            chunks.add(chunk)
        }

        return chunks
    }

    class Chunk(val opcode: Int, val data: ByteArray? = null) {

        override fun toString(): String {
            val buf = StringBuilder()
            //  opcode is a single byte of non-pushdata content
            when {
                opcode > OP_PUSHDATA4 -> {
                    buf.append(OpCodes.getOpCodeName(opcode))
                }
                data != null -> {
                    // Data chunk
                    buf.append(OpCodes.getPushDataName(opcode))
                            .append("[")
                            .append(data.toHexString())
                            .append("]")
                }
                else -> {
                    // Small num
                    buf.append(opcode)
                }
            }

            return buf.toString()
        }
    }

    override fun toString() = chunks.joinToString(" ")
}
