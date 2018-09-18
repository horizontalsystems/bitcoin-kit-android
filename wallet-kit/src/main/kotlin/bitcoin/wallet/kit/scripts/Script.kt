package bitcoin.wallet.kit.scripts

import bitcoin.walllet.kit.utils.HashUtils

enum class ScriptType { P2PKH, P2PK, P2SH, UNKNOWN }

class Script(bytes: ByteArray) {
    var chunks = listOf<ScriptChunk>()

    // Creation time of the associated keys in seconds since the epoch.
    private var creationTimeSeconds: Long

    init {
        chunks = ScriptParser.parseChunks(bytes)
        creationTimeSeconds = 0
    }

    fun getPubKeyHash(): ByteArray? {
        if (ScriptParser.isP2PKH(this))
            return chunks[2].data
        if (ScriptParser.isP2PK(this))
            return HashUtils.ripeMd160(chunks[0].data)
        if (ScriptParser.isP2SH(this))
            return chunks[1].data

        return null
    }

    fun getScriptType(): ScriptType {
        if (ScriptParser.isP2PKH(this))
            return ScriptType.P2PKH
        if (ScriptParser.isP2PK(this))
            return ScriptType.P2PK
        if (ScriptParser.isP2SH(this))
            return ScriptType.P2SH

        return ScriptType.UNKNOWN
    }

    override fun toString() = chunks.joinToString(" ")
}
