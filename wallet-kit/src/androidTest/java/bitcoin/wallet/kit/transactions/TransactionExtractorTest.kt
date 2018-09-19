package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.TestData
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TransactionExtractorTest {
    lateinit var transactionP2PKH: Transaction
    lateinit var transactionP2PK: Transaction
    lateinit var transactionP2SH: Transaction

    lateinit var output1: TransactionOutput
    lateinit var output2: TransactionOutput

    lateinit var extractor: TransactionExtractor

    @Before
    fun setup() {
        extractor = TransactionExtractor()
    }

    @Test
    fun extractP2PKH() {
        transactionP2PKH = TestData.transactionP2PKH

        output1 = transactionP2PKH.outputs[0]!!
        output2 = transactionP2PKH.outputs[1]!!

        assertNull(output1.keyHash)
        assertNull(output2.keyHash)

        extractor.extract(transactionP2PKH)

        assertEquals(ScriptType.P2PKH, output1.scriptType)
        assertEquals(ScriptType.P2PKH, output2.scriptType)
        assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", output1.keyHash?.toHexString())
        assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", output2.keyHash?.toHexString())
    }

    @Test
    fun extractP2SH() {
        transactionP2SH = TestData.transactionP2SH

        output1 = transactionP2SH.outputs[0]!!
        output2 = transactionP2SH.outputs[1]!!

        assertNull(output1.keyHash)
        assertNull(output2.keyHash)

        extractor.extract(transactionP2SH)

        assertEquals(ScriptType.P2SH, output1.scriptType)
        assertEquals(ScriptType.P2SH, output2.scriptType)
        assertEquals("14020b83b8c2e50152259f3fe5ccfe206c7842d7", output1.keyHash?.toHexString())
        assertEquals("74cc925ea13e9e2e323055afa5e59f715f62036e", output2.keyHash?.toHexString())
    }

    @Test
    fun extractP2PK() {
        transactionP2PK = TestData.transactionP2PK

        output1 = transactionP2PK.outputs[0]!!
        output2 = transactionP2PK.outputs[1]!!

        assertNull(output1.keyHash)
        assertNull(output2.keyHash)

        extractor.extract(transactionP2PK)

        assertEquals(ScriptType.P2PK, output1.scriptType)
        assertEquals("fc916f213a3d7f1369313d5fa30f6168f9446a2d", output1.keyHash?.toHexString())
    }
}
