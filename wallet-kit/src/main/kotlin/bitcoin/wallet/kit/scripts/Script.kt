package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.scripts.OpCodes.getOpCodeName
import bitcoin.walllet.kit.hdwallet.Utils
import java.io.ByteArrayInputStream

class Script(bytes: ByteArray) {
    private var chunks = mutableListOf<ScriptChunk>()

    // Creation time of the associated keys in seconds since the epoch.
    private var creationTimeSeconds: Long

    private val standardScriptChunks = arrayOf(
            ScriptChunk(OP_DUP, null, 0),
            ScriptChunk(OP_HASH160, null, 1),
            ScriptChunk(OP_EQUALVERIFY, null, 23),
            ScriptChunk(OP_CHECKSIG, null, 24)
    )

    init {
        parse(bytes)
        creationTimeSeconds = 0
    }

    fun getPubKeyHash(): ByteArray {
        return chunks.getOrNull(2)?.data ?: byteArrayOf()
    }

    private fun parse(bytes: ByteArray) {
        chunks = mutableListOf() // Common size.

        val stream = ByteArrayInputStream(bytes)
        val initialSize = stream.available()

        while (stream.available() > 0) {
            val startLocationInScript = initialSize - stream.available()
            val opcode = stream.read()

            var dataToRead: Long = -1

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

            var chunk: ScriptChunk
            if (dataToRead < 0) {
                chunk = ScriptChunk(opcode, null, startLocationInScript)
            } else if (dataToRead > stream.available()) {
                throw Exception("Push of data element that is larger than remaining data")
            } else {
                val data = ByteArray(dataToRead.toInt())
                check(dataToRead == 0L || stream.read(data, 0, dataToRead.toInt()).toLong() == dataToRead)
                chunk = ScriptChunk(opcode, data, startLocationInScript)
            }

            // Save some memory by eliminating redundant copies of the same chunk objects.
            for (sc in standardScriptChunks) {
                if (sc == chunk) chunk = sc
            }

            chunks.add(chunk)
        }
    }

    override fun toString() = chunks.joinToString(" ")

    companion object {

        fun decodeFromOpN(opcode: Int): Int {
            check(opcode == OP_0 || opcode in OP_1..OP_16 || opcode == OP_1NEGATE) {
                "decodeFromOpN called on non OP_N opcode: ${getOpCodeName(opcode)}"
            }

            return when (opcode) {
                OP_0 -> 0
                OP_1NEGATE -> -1
                else -> opcode + 1 - OP_1
            }
        }
    }
}
