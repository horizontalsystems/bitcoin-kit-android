package io.horizontalsystems.bitcoincore.transactions.scripts

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.utils.Utils
import java.io.ByteArrayInputStream

// TODO: make it enum
object ScriptType {
    const val P2PKH = 1     // pay to pubkey hash (aka pay to address)
    const val P2PK = 2      // pay to pubkey
    const val P2SH = 3      // pay to script hash
    const val P2WPKH = 4    // pay to witness pubkey hash
    const val P2WSH = 5     // pay to witness script hash
    const val P2WPKHSH = 6  // P2WPKH nested in P2SH
    const val NULL_DATA = 7
    const val UNKNOWN = 0
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
            if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                // Read some bytes of data, where how many is the opcode value itself.
                dataToRead = opcode.toLong()
            } else if (opcode == OP_PUSHDATA1) {
                if (stream.available() < 1) throw Exception("Unexpected end of script")

                dataToRead = stream.read().toLong()
            } else if (opcode == OP_PUSHDATA2) {
                // Read a short, then read that many bytes of data.
                if (stream.available() < 2) throw Exception("Unexpected end of script")

                dataToRead = Utils.readUint16FromStream(stream).toLong()
            } else if (opcode == OP_PUSHDATA4) {
                // Read a uint32, then read that many bytes of data.
                // Though this is allowed, because its value cannot be > 520, it should never actually be used
                if (stream.available() < 4) throw Exception("Unexpected end of script")

                dataToRead = Utils.readUint32FromStream(stream)
            }

            var chunk: Chunk
            if (dataToRead < 0) {
                chunk = Chunk(opcode)
            } else if (dataToRead > stream.available()) {
                throw Exception("Push of data element that is larger than remaining data")
            } else {
                val data = ByteArray(dataToRead.toInt())
                check(dataToRead == 0L || stream.read(data, 0, dataToRead.toInt()).toLong() == dataToRead)
                chunk = Chunk(opcode, data)
            }

            chunks.add(chunk)
        }

        return chunks
    }

    class Chunk(val opcode: Int, val data: ByteArray? = null) {

        override fun toString(): String {
            val buf = StringBuilder()
            //  opcode is a single byte of non-pushdata content
            if (opcode > OP_PUSHDATA4) {
                buf.append(OpCodes.getOpCodeName(opcode))
            } else if (data != null) {
                // Data chunk
                buf.append(OpCodes.getPushDataName(opcode))
                        .append("[")
                        .append(data.toHexString())
                        .append("]")
            } else {
                // Small num
                buf.append(opcode)
            }

            return buf.toString()
        }
    }

    override fun toString() = chunks.joinToString(" ")
}
