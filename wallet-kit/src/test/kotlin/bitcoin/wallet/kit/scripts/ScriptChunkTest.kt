package bitcoin.wallet.kit.scripts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptChunkTest {

    @Test
    fun test() {
        assertEquals(OP_DUP, 0x76)
        assertEquals(OpCodes.getOpCodeName(OP_DUP), "DUP")
        assertEquals(OpCodes.getPushDataName(OP_DUP), "DUP")
        assertEquals(OpCodes.getOpCode("DUP"), OP_DUP)
    }

    @Test
    fun isOpcodeDisabled() {
        fun isDisabled(opcode: Int): Boolean =
                ScriptChunk(opcode, null).isOpcodeDisabled()

        assertTrue(isDisabled(OP_CAT))
        assertTrue(isDisabled(OP_SUBSTR))
        assertTrue(isDisabled(OP_LEFT))
        assertTrue(isDisabled(OP_RIGHT))
        assertTrue(isDisabled(OP_INVERT))
        assertTrue(isDisabled(OP_AND))
        assertTrue(isDisabled(OP_OR))
        assertTrue(isDisabled(OP_XOR))
        assertTrue(isDisabled(OP_2MUL))
        assertTrue(isDisabled(OP_2DIV))
        assertTrue(isDisabled(OP_MUL))
        assertTrue(isDisabled(OP_DIV))
        assertTrue(isDisabled(OP_MOD))
        assertTrue(isDisabled(OP_LSHIFT))
        assertTrue(isDisabled(OP_RSHIFT))
    }
}
