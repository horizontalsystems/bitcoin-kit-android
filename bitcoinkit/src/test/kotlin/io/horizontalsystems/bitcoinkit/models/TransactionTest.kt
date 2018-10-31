package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionTest {
    private val txHashLE = "169e1e83e930853391bc6f35f605c6754cfead57cf8387639d3b4096c54f18f4"
    private val txHashBE = "f4184fc596403b9d638783cf57adfe4c75c605f6356fbc91338530e9831e9e16"
    private var txRaw = "0100000001c997a5e56e104102fa209c6a852dd90660a20b2d9c352423edce25857fcd3704000000004847304402204e45e16932b8af514961a1d3a1a25fdf3f4f7732e9d624c6c61548ab5fb8cd410220181522ec8eca07de4860a4acdd12909d831cc56cbbac4622082221a8768d1d0901ffffffff0200ca9a3b00000000434104ae1a62fe09c5f51b13905f07f06b99a2f7159b2225f374cd378d71302fa28414e7aab37397f554a7df5f142c21c1b7303b8a0626f1baded5c72a704f7e6cd84cac00286bee0000000043410411db93e1dcdb8a016b49840f8c53bc1eb68a382e97b1482ecad7b148a6909a5cb2e0eaddfb84ccf9744464f82e160bfa9b8b64f9d4c03f999b8643f656b412a3ac00000000"

    lateinit var transaction: Transaction

    @Test
    fun init() {
        transaction = Transaction(BitcoinInput(txRaw.hexStringToByteArray()))

        assertEquals(transaction.inputs.size, 1)
        assertEquals(transaction.outputs.size, 2)

        assertEquals(transaction.hash.toHexString(), txHashLE)
        assertEquals(transaction.hashHexReversed, txHashBE)
    }

    @Test
    fun init_witness() {
        val witness = arrayOf(
                "3045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb191901",
                "038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac"
        )
        txRaw = "0100000000010115e180dc28a2327e687facc33f10f2a20da717e5548406f7ae8b4c811072f8560100000000ffffffff0100b4f505000000001976a9141d7cd6c75c2e86f4cbf98eaed221b30bd9a0b92888ac0248${witness[0]}21${witness[1]}00000000"
        transaction = Transaction(BitcoinInput(txRaw.hexStringToByteArray()))

        val inputs = transaction.inputs
        assertEquals(inputs.size, 1)
        assertEquals(transaction.outputs.size, 1)

        assertEquals(transaction.segwit, true)
        assertEquals(inputs[0]!!.witness[0]?.toHexString(), witness[0])
        assertEquals(inputs[0]!!.witness[1]?.toHexString(), witness[1])
    }

    @Test
    fun toByteArray_witness() {
        txRaw = "0100000000010115e180dc28a2327e687facc33f10f2a20da717e5548406f7ae8b4c811072f8560100000000ffffffff0100b4f505000000001976a9141d7cd6c75c2e86f4cbf98eaed221b30bd9a0b92888ac02483045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb19190121038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac00000000"
        transaction = Transaction(BitcoinInput(txRaw.hexStringToByteArray()))

        assertEquals(transaction.inputs.size, 1)
        assertEquals(transaction.outputs.size, 1)

        assertEquals(transaction.segwit, true)

        assertEquals(txRaw, transaction.toByteArray().toHexString())
    }

}
