package io.horizontalsystems.bitcoinkit.transactions.scripts

import io.horizontalsystems.bitcoinkit.core.toHexString

class ScriptChunk(val opcode: Int, val data: ByteArray? = null) {

    fun equalsOpCode(opcode: Int): Boolean {
        return opcode == this.opcode
    }

    // If this chunk is a single byte of non-pushdata content
    fun isOpCode(): Boolean {
        return opcode > OP_PUSHDATA4
    }

    fun isOpcodeDisabled(): Boolean {
        return (opcode == OP_CAT ||
                opcode == OP_SUBSTR ||
                opcode == OP_LEFT ||
                opcode == OP_RIGHT ||
                opcode == OP_INVERT ||
                opcode == OP_AND ||
                opcode == OP_OR ||
                opcode == OP_XOR ||
                opcode == OP_2MUL ||
                opcode == OP_2DIV ||
                opcode == OP_MUL ||
                opcode == OP_DIV ||
                opcode == OP_MOD ||
                opcode == OP_LSHIFT ||
                opcode == OP_RSHIFT)
    }

    override fun toString(): String {
        val buf = StringBuilder()
        if (isOpCode()) {
            buf.append(OpCodes.getOpCodeName(opcode))
        } else if (data != null) {
            // Data chunk
            buf.append(OpCodes.getPushDataName(opcode))
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
