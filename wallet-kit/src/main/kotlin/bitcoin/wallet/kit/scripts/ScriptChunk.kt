package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.toHexString

class ScriptChunk(val opcode: Int, val data: ByteArray? = null, val startLocationInScript: Int = -1) {

    fun equalsOpCode(opcode: Int): Boolean {
        return opcode == this.opcode
    }

    // If this chunk is a single byte of non-pushdata content
    fun isOpCode(): Boolean {
        return opcode > OP_PUSHDATA4
    }

    override fun toString(): String {
        val buf = StringBuilder()
        if (isOpCode()) {
            buf.append(OpCodes.getOpCodeName(opcode))
        } else if (data != null) {
            // Data chunk
            buf.append(OpCodes
                    .getPushDataName(opcode))
                    .append("[")
                    .append(data.toHexString())
                    .append("]")
        } else {
            // Small num
            buf.append(ScriptParser.decodeFromOpN(opcode))
        }

        return buf.toString()
    }
}
