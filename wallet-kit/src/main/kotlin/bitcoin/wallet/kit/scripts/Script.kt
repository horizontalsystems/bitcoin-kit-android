package bitcoin.wallet.kit.scripts

import bitcoin.walllet.kit.hdwallet.Utils

object ScriptType {
    const val P2PKH = 1 // pay to pubkey hash (aka pay to address)
    const val P2PK = 2  // pay to pubkey
    const val P2SH = 3  // pay to script hash
    const val UNKNOWN = 0
}

class Script(bytes: ByteArray) {
    var chunks = listOf<ScriptChunk>()

    // Creation time of the associated keys in seconds since the epoch.
    private var creationTimeSeconds: Long

    init {
        chunks = ScriptParser.parseChunks(bytes)
        creationTimeSeconds = 0
    }

    fun getPubKeyHashIn(): ByteArray? {
        if (ScriptParser.isPKHashInput(this))
            return Utils.sha256Hash160(chunks[1].data)
        if (ScriptParser.isSHashInput(this) || ScriptParser.isMultiSigInput(this))
            return Utils.sha256Hash160(chunks.last().data)

        return null
    }

    fun getPubKeyHash(): ByteArray? {
        if (ScriptParser.isP2PKH(this))
            return chunks[2].data
        if (ScriptParser.isP2PK(this))
            return Utils.sha256Hash160(chunks[0].data)
        if (ScriptParser.isP2SH(this))
            return chunks[1].data

        return null
    }

    fun getScriptType(): Int {
        if (ScriptParser.isP2PKH(this) || ScriptParser.isPKHashInput(this))
            return ScriptType.P2PKH
        if (ScriptParser.isP2PK(this) || ScriptParser.isPubKeyInput(this))
            return ScriptType.P2PK
        if (ScriptParser.isP2SH(this) || ScriptParser.isMultiSigInput(this))
            return ScriptType.P2SH

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
