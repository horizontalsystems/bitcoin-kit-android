package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.utils.HashUtils
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionTest : Spek({

    lateinit var transaction: Transaction
    lateinit var fullTransaction: FullTransaction

    val txHashLE = "169e1e83e930853391bc6f35f605c6754cfead57cf8387639d3b4096c54f18f4"
    var txRaw = "0100000001c997a5e56e104102fa209c6a852dd90660a20b2d9c352423edce25857fcd3704000000004847304402204e45e16932b8af514961a1d3a1a25fdf3f4f7732e9d624c6c61548ab5fb8cd410220181522ec8eca07de4860a4acdd12909d831cc56cbbac4622082221a8768d1d0901ffffffff0200ca9a3b00000000434104ae1a62fe09c5f51b13905f07f06b99a2f7159b2225f374cd378d71302fa28414e7aab37397f554a7df5f142c21c1b7303b8a0626f1baded5c72a704f7e6cd84cac00286bee0000000043410411db93e1dcdb8a016b49840f8c53bc1eb68a382e97b1482ecad7b148a6909a5cb2e0eaddfb84ccf9744464f82e160bfa9b8b64f9d4c03f999b8643f656b412a3ac00000000"

    describe("constructor") {

        it("init") {
            fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(txRaw.hexToByteArray()))
            transaction = fullTransaction.header

            assertEquals(fullTransaction.inputs.size, 1)
            assertEquals(fullTransaction.outputs.size, 2)

            assertEquals(fullTransaction.outputs[0].index, 0)
            assertEquals(fullTransaction.outputs[1].index, 1)

            assertEquals(transaction.hash.toHexString(), txHashLE)
        }

        it("withWitness") {
            val witness = arrayOf(
                    "3045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb191901",
                    "038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac"
            )

            txRaw = "0100000000010115e180dc28a2327e687facc33f10f2a20da717e5548406f7ae8b4c811072f8560100000000ffffffff0100b4f505000000001976a9141d7cd6c75c2e86f4cbf98eaed221b30bd9a0b92888ac0248${witness[0]}21${witness[1]}00000000"
            fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(txRaw.hexToByteArray()))
            transaction = fullTransaction.header

            val inputs = fullTransaction.inputs
            assertEquals(inputs.size, 1)
            assertEquals(fullTransaction.outputs.size, 1)

            assertEquals(transaction.segwit, true)
            assertEquals(inputs[0].witness[0].toHexString(), witness[0])
            assertEquals(inputs[0].witness[1].toHexString(), witness[1])
        }

        it("toByteArray_witness") {
            txRaw = "0100000000010115e180dc28a2327e687facc33f10f2a20da717e5548406f7ae8b4c811072f8560100000000ffffffff0100b4f505000000001976a9141d7cd6c75c2e86f4cbf98eaed221b30bd9a0b92888ac02483045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb19190121038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac00000000"
            fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(txRaw.hexToByteArray()))
            transaction = fullTransaction.header

            assertEquals(fullTransaction.inputs.size, 1)
            assertEquals(fullTransaction.outputs.size, 1)

            assertEquals(transaction.segwit, true)

            assertEquals(txRaw, TransactionSerializer.serialize(fullTransaction).toHexString())
        }

        it("withoutWitness") {
            val txHash = "8530de8e771c830c4e76909b1fdf0e055ee8872fa1ebbf7c4279375591061a62"
            val txWitnessHash = "f5b5c6fc1b199ecfa04d192a9f81a9cc6ec78bd848e163a80b2d077b4fe1d097"

            txRaw = "01000000000101dbf198515cebea6e248a212c63299e63a2a35a2def0a42e43e0106c2efff12860100000000ffffffff02e6988102000000001976a914d1b4380d709e9ea54943a083b1208d6d991893d988ac58271101000000001600149063d7cc1cf2d55f6c0076e65587c755dbe96ed702483045022100fa18145855d55b221c0df4cd72b12dcb26f451aa4b8ca2148ef535d3e374baff02205b13c14fbd8665be6a6da4fb65b46a737679e988956ec353a7c7e40cbe43f7a40121038d0705f4511adf850b16baf4f689d3d92fe63cc9a5f6d5d00d2e4ed699e511f800000000"

            fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(txRaw.hexToByteArray()))
            transaction = fullTransaction.header

            assertEquals(txHash, transaction.hash.toReversedHex())

            val bytes = HashUtils.doubleSha256(TransactionSerializer.serialize(fullTransaction, withWitness = true))

            assertEquals(txWitnessHash, bytes.toReversedHex())
        }


        it("withoutWitnes") {
            val txHash = "8530de8e771c830c4e76909b1fdf0e055ee8872fa1ebbf7c4279375591061a62"
            val txWitnessHash = "f5b5c6fc1b199ecfa04d192a9f81a9cc6ec78bd848e163a80b2d077b4fe1d097"

            txRaw = "01000000000101dbf198515cebea6e248a212c63299e63a2a35a2def0a42e43e0106c2efff12860100000000ffffffff02e6988102000000001976a914d1b4380d709e9ea54943a083b1208d6d991893d988ac58271101000000001600149063d7cc1cf2d55f6c0076e65587c755dbe96ed702483045022100fa18145855d55b221c0df4cd72b12dcb26f451aa4b8ca2148ef535d3e374baff02205b13c14fbd8665be6a6da4fb65b46a737679e988956ec353a7c7e40cbe43f7a40121038d0705f4511adf850b16baf4f689d3d92fe63cc9a5f6d5d00d2e4ed699e511f800000000"

            fullTransaction = TransactionSerializer.deserialize(BitcoinInputMarkable(txRaw.hexToByteArray()))

            val bytes = HashUtils.doubleSha256(TransactionSerializer.serialize(fullTransaction, true))

            assertEquals(txWitnessHash, bytes.toReversedHex())
        }

    }
})
