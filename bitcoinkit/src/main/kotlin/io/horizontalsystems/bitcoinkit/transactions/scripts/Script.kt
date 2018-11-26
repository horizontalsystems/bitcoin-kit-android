package io.horizontalsystems.bitcoinkit.transactions.scripts

import io.horizontalsystems.bitcoinkit.utils.Utils

object ScriptType {
    const val P2PKH = 1     // pay to pubkey hash (aka pay to address)
    const val P2PK = 2      // pay to pubkey
    const val P2SH = 3      // pay to script hash
    const val P2WPKH = 4    // pay to witness pubkey hash
    const val P2WSH = 5     // pay to witness script hash
    const val P2WPKHSH = 6  // P2WPKH nested in P2SH
    const val UNKNOWN = 0
}

class Script(private val bytes: ByteArray) {
    val chunks = try {
        ScriptParser.parseChunks(bytes)
    } catch (e: Exception) {
        listOf<ScriptChunk>()
    }

    fun getPubKeyHashIn(): ByteArray? {
        if (ScriptParser.isPKHashInput(this))
            return Utils.sha256Hash160(chunks[1].data)
        if (ScriptParser.isSHashInput(this) || ScriptParser.isMultiSigInput(this))
            return Utils.sha256Hash160(chunks.last().data)
        if (ScriptParser.isP2WPKH(this))
            return bytes

        return null
    }

    fun getPubKeyHash(): ByteArray? {
        if (ScriptParser.isP2PKH(this))
            return chunks[2].data
        if (ScriptParser.isP2PK(this))
            return Utils.sha256Hash160(chunks[0].data)
        if (ScriptParser.isP2SH(this))
            return chunks[1].data
        if (ScriptParser.isPayToWitnessHash(this))
            return bytes

        return null
    }

    fun getScriptType(): Int {
        if (ScriptParser.isP2PKH(this) || ScriptParser.isPKHashInput(this))
            return ScriptType.P2PKH
        if (ScriptParser.isP2PK(this) || ScriptParser.isPubKeyInput(this))
            return ScriptType.P2PK
        if (ScriptParser.isP2SH(this) || ScriptParser.isMultiSigInput(this))
            return ScriptType.P2SH
        if (ScriptParser.isP2WPKH(this))
            return ScriptType.P2WPKH
        if (ScriptParser.isP2WSH(this))
            return ScriptType.P2WSH

        return ScriptType.UNKNOWN
    }

    fun isCode(): Boolean {
        for (chunk in chunks) {
            if (chunk.opcode == -1)
                return false

            if (chunk.isOpcodeDisabled())
                return false

            if (chunk.opcode == OP_RESERVED || chunk.opcode == OP_NOP || chunk.opcode == OP_VER || chunk.opcode == OP_VERIF || chunk.opcode == OP_VERNOTIF || chunk.opcode == OP_RESERVED1 || chunk.opcode == OP_RESERVED2 || chunk.opcode == OP_NOP1)
                return false

            if (chunk.opcode > OP_CHECKSEQUENCEVERIFY)
                return false
        }

        return true
    }

    fun isPushOnly(): Boolean {
        for (chunk in chunks) {
            if (chunk.opcode == -1)
                return false

            if (chunk.opcode > OP_16)
                return false
        }

        return true
    }

    override fun toString() = chunks.joinToString(" ")
}
