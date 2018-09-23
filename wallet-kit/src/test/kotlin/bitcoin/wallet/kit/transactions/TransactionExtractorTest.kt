package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.TestNet
import bitcoin.wallet.kit.scripts.ScriptType
import helpers.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TransactionExtractorTest {
    lateinit var transaction: Transaction
    lateinit var extractor: TransactionExtractor

    @Before
    fun setup() {
        extractor = TransactionExtractor(TestNet())
    }

    @Test
    fun extractP2PKH() {
        transaction = Fixtures.transactionP2PKH

        assertNull(transaction.inputs[0]?.keyHash)
        assertNull(transaction.outputs[0]?.keyHash)
        assertNull(transaction.outputs[1]?.keyHash)

        extractor.extract(transaction)

        // output
        assertEquals(ScriptType.P2PKH, transaction.outputs[0]?.scriptType)
        assertEquals(ScriptType.P2PKH, transaction.outputs[1]?.scriptType)
        assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", transaction.outputs[0]?.keyHash?.toHexString())
        assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", transaction.outputs[1]?.keyHash?.toHexString())
        // input
        assertEquals("f6889a22593e9156ef80bdcda0e1b355e8949e05", transaction.inputs[0]?.keyHash?.toHexString())
        // address
        assertEquals("n3zWAXKu6LBa8qYGEuTEfg9RXeijRHj5rE", transaction.inputs[0]?.address)
        assertEquals("mkbGp1uE1jRfdNxtWAUTGWKc9r2pRsLiUi", transaction.outputs[0]?.address)
        assertEquals("mkbGp1uE1jRfdNxtWAUTGWKc9r2pRsLiUi", transaction.outputs[1]?.address)
    }

    @Test
    fun extractP2SH() {
        transaction = Fixtures.transactionP2SH

        assertNull(transaction.inputs[0]?.keyHash)
        assertNull(transaction.outputs[0]?.keyHash)
        assertNull(transaction.outputs[1]?.keyHash)

        extractor.extract(transaction)

        // output
        assertEquals(ScriptType.P2SH, transaction.outputs[0]?.scriptType)
        assertEquals(ScriptType.P2SH, transaction.outputs[1]?.scriptType)
        assertEquals("cdfb2eb01489e9fe8bd9b878ce4a7084dd887764", transaction.outputs[0]?.keyHash?.toHexString())
        assertEquals("aed6f804c63da80800892f8fd4cdbad0d3ad6d12", transaction.outputs[1]?.keyHash?.toHexString())
        // input
        assertEquals("aed6f804c63da80800892f8fd4cdbad0d3ad6d12", transaction.inputs[0]?.keyHash?.toHexString())
        // address
        assertEquals("2N9Bh5xXL1CdQohpcqPiphdqtQGuAquWuaG", transaction.inputs[0]?.address)
        assertEquals("2NC2MR4p1VsHCgAAo8C5KPmyKhuY6rb6SGN", transaction.outputs[0]?.address)
        assertEquals("2N9Bh5xXL1CdQohpcqPiphdqtQGuAquWuaG", transaction.outputs[1]?.address)
    }

    @Test
    fun extractP2PK() {
        transaction = Fixtures.transactionP2PK

        assertNull(transaction.inputs[0]?.keyHash)
        assertNull(transaction.outputs[0]?.keyHash)
        assertNull(transaction.outputs[1]?.keyHash)

        extractor.extract(transaction)

        assertEquals(ScriptType.P2PK, transaction.outputs[0]?.scriptType)
        assertEquals("fc916f213a3d7f1369313d5fa30f6168f9446a2d", transaction.outputs[0]?.keyHash?.toHexString())
        // address
        assertEquals("", transaction.inputs[0]?.address)
        assertEquals("n4YQoLK25P4RsJ2wJEpKnT6q2WGxt149rs", transaction.outputs[0]?.address)
        assertEquals("mh8YhPYEAYs3E7EVyKtB5xrcfMExkkdEMF", transaction.outputs[1]?.address)
    }
}
