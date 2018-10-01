package bitcoin.wallet.kit.scripts

import bitcoin.walllet.kit.hdwallet.ECKey
import bitcoin.walllet.kit.utils.Utils
import java.io.ByteArrayInputStream

object ScriptParser {

    private const val ADDRESS_LENGTH = 20
    private val standardScriptChunks = arrayOf(
            ScriptChunk(OP_DUP, null, 0),
            ScriptChunk(OP_HASH160, null, 1),
            ScriptChunk(OP_EQUALVERIFY, null, 23),
            ScriptChunk(OP_CHECKSIG, null, 24)
    )

    fun parseChunks(bytes: ByteArray): List<ScriptChunk> {
        val chunks = mutableListOf<ScriptChunk>() // Common size.
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

        return chunks
    }

    fun isPKHashInput(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size != 2)
            return false
        val sig = chunks[0].data
        val key = chunks[1].data
        if (key == null || sig == null)
            return false

        return sig.size in 9..73 && (key.size == 33 || key.size == 65)
    }

    fun isPubKeyInput(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size != 1)
            return false

        val chunk0 = chunks[0].data
        if (chunk0 == null)
            return false

        return chunk0.size in 9..73
    }

    fun isSHashInput(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.isEmpty())
            return false

        // Grab the raw redeem script
        val redeemChunk = chunks.last()
        if (redeemChunk.data == null)
            return false

        if (ECKey.isSignatureCanonical(redeemChunk.data))
            return false
        if (ECKey.isPubKeyCanonical(redeemChunk.data))
            return false

        val redeemScript = Script(redeemChunk.data)

        if (!redeemScript.isCode())
            return false

        if (!redeemScript.isPushOnly())
            return false

        return true
    }

    fun isMultiSigInput(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size < 2)
            return false

        if (!chunks[0].equalsOpCode(OP_0))
            return false

        if (chunks[1].isOpCode())
            return false

        if (isSHashInput(script))
            return false

        val redeemChunk = chunks.last()
        if (redeemChunk.data == null)
            return false

        val redeemScript = Script(redeemChunk.data)
        val chunkLast = redeemScript.chunks.last()
        if (chunkLast.equalsOpCode(OP_ENDIF)) {
            // handle
        }

        return chunkLast.equalsOpCode(OP_CHECKSIG) || chunkLast.equalsOpCode(OP_CHECKSIGVERIFY) || chunkLast.equalsOpCode(OP_CHECKMULTISIGVERIFY) || chunkLast.equalsOpCode(OP_CHECKMULTISIG)
    }

    // Pay To PubKey Hash; OP_DUP OP_HASH160 <pubkey hash> OP_EQUALVERIFY OP_CHECKSIG
    fun isP2PKH(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size != 5)
            return false
        if (!chunks[0].equalsOpCode(OP_DUP))
            return false
        if (!chunks[1].equalsOpCode(OP_HASH160))
            return false
        val chunk2data = chunks[2].data ?: return false
        if (chunk2data.size != 20)
            return false
        if (!chunks[3].equalsOpCode(OP_EQUALVERIFY))
            return false
        if (!chunks[4].equalsOpCode(OP_CHECKSIG))
            return false

        return true
    }

    // Pay To PubKey; <pubkey> OP_CHECKSIG
    fun isP2PK(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size != 2)
            return false
        val chunk0 = chunks[0]
        if (chunk0.isOpCode())
            return false
        val chunk0data = chunk0.data ?: return false
        if (chunk0data.size <= 1)
            return false
        if (!chunks[1].equalsOpCode(OP_CHECKSIG))
            return false

        return true
    }

    // Pay To ScriptHash; OP_HASH160 OP_PUSHDATA1 0x14 <20 bytes of script hash> OP_EQUAL
    fun isP2SH(script: Script): Boolean {
        val chunks = script.chunks
        if (chunks.size != 3)
            return false
        if (!chunks[0].equalsOpCode(OP_HASH160))
            return false
        val chunk1 = chunks[1]
        if (chunk1.opcode != 0x14)
            return false
        val chunk1data = chunk1.data ?: return false
        if (chunk1data.size != ADDRESS_LENGTH)
            return false
        if (!chunks[2].equalsOpCode(OP_EQUAL))
            return false

        return true
    }

    fun decodeFromOpN(opcode: Int): Int {
        check(opcode == OP_0 || opcode in OP_1..OP_16 || opcode == OP_1NEGATE) {
            "decodeFromOpN called on non OP_N opcode: ${OpCodes.getOpCodeName(opcode)}"
        }

        return when (opcode) {
            OP_0 -> 0
            OP_1NEGATE -> -1
            else -> opcode + 1 - OP_1
        }
    }

}
