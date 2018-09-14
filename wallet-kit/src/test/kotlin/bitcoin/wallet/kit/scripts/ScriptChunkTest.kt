package bitcoin.wallet.kit.scripts

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptChunkTest {

    @Test
    fun test() {
        assertEquals(OP_DUP, 0x76)
        assertEquals(OpCodes.getOpCodeName(OP_DUP), "DUP")
        assertEquals(OpCodes.getPushDataName(OP_DUP), "DUP")
        assertEquals(OpCodes.getOpCode("DUP"), OP_DUP)
    }

}
