package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.toHexString

class ScriptChunk(private val opcode: Int, val data: ByteArray? = null, val startLocationInScript: Int = -1) {

    // If this chunk is a single byte of non-pushdata content
    private fun isOpCode(): Boolean {
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
            buf.append(Script.decodeFromOpN(opcode))
        }

        return buf.toString()
    }
}
