package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import org.junit.Assert.*
import org.mockito.Mockito
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionExtractorTest : Spek({
    val storage = Mockito.mock(IStorage::class.java)
    val addressConverter = Mockito.mock(IAddressConverter::class.java)
    val pluginManager = mock<PluginManager>()

    lateinit var transactionOutput: TransactionOutput
    lateinit var transactionInput: TransactionInput
    lateinit var fullTransaction: FullTransaction
    lateinit var extractor: TransactionExtractor
    lateinit var transactionOutputsCache: OutputsCache

    beforeEachTest {
        transactionOutput = TransactionOutput()
        transactionInput = TransactionInput(byteArrayOf(), 0)
        fullTransaction = FullTransaction(Transaction(), listOf(transactionInput), listOf(transactionOutput))
        transactionOutputsCache = OutputsCache()

        extractor = TransactionExtractor(addressConverter, storage, pluginManager, transactionOutputsCache)
    }

    describe("#extract") {

        //
        // Input
        //

        it("extractInputs_P2SH") {
            val address = LegacyAddress("00112233", byteArrayOf(1), AddressType.P2SH)
            val signScript = "004830450221008c203a0881f75c731d9a3a2e6d2ffa37da7095b7dde61a9e7a906659219cd0fa02202677097ca7f7e164f73924fe8f84e1e6fc6611450efcda360ce771e98af9f73d0147304402201cba9b641483476f67a4cef08d7280f51de8d7615fcce76642d944dc07132a990220323d13175477bbf67c8c36fb243bec0e4c410bc9173a186d9f8e98ce3445363601475221025b64f7c63e30f315259393f64dcca269d18386997b1cc93da1388c4021e3ea8e210386d42d5d7027ac08ddcbb066e2140575091fe7dc1d202a008eb5e036725e975652ae"

            whenever(addressConverter.convert(any<ByteArray>(), any())).thenReturn(address)

            transactionInput.sigScript = signScript.hexToByteArray()
            extractor.extractInputs(fullTransaction)

            assertEquals(address.hash, fullTransaction.inputs[0].keyHash)
            assertEquals(address.string, fullTransaction.inputs[0].address)
        }

        it("extractInputs_P2PKH") {
            val address = LegacyAddress("00112233", byteArrayOf(1), AddressType.P2PKH)
            val signScript = "483045022100907103d70cd2215bc76e27e07cafa39e975cbf4a7f5897402883dbd59b42ed5e022000bbaeb898d2f5c687a420ad51e001080035ee9690b19d6af4bc192f1e0a8b17012103aac540428b6955a53bb01fcae6d4279df45253b2c61684fb993b5545935dac7a"

            whenever(addressConverter.convert(any<ByteArray>(), any())).thenReturn(address)

            transactionInput.sigScript = signScript.hexToByteArray()
            extractor.extractInputs(fullTransaction)

            assertEquals(address.hash, fullTransaction.inputs[0].keyHash)
            assertEquals(address.string, fullTransaction.inputs[0].address)
        }

        it("extractInputs_P2WPKHSH") {
            val address = LegacyAddress("00112233", byteArrayOf(1), AddressType.P2SH)
            val signScript = "1600148749115073ad59a6f3587f1f9e468adedf01473f"

            whenever(addressConverter.convert(any<ByteArray>(), any())).thenReturn(address)

            transactionInput.sigScript = signScript.hexToByteArray()
            extractor.extractInputs(fullTransaction)

            assertEquals(address.hash, fullTransaction.inputs[0].keyHash)
            assertEquals(address.string, fullTransaction.inputs[0].address)
        }

        //
        // Output
        //

        it("extractOutputs_P2PKH") {
            assertNull(fullTransaction.outputs[0].keyHash)

            val keyHash = "1ec865abcb88cec71c484d4dadec3d7dc0271a7b"
            transactionOutput.lockingScript = "76a914${keyHash}88AC".hexToByteArray()
            extractor.extractOutputs(fullTransaction)

            assertEquals(keyHash, fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals(ScriptType.P2PKH, fullTransaction.outputs[0].scriptType)
        }

        it("extractOutputs_P2PK") {
            assertNull(fullTransaction.outputs[0].keyHash)

            val keyHash = "037d56797fbe9aa506fc263751abf23bb46c9770181a6059096808923f0a64cb15"
            transactionOutput.lockingScript = "21${keyHash}AC".hexToByteArray()
            extractor.extractOutputs(fullTransaction)

            assertEquals(keyHash, fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals(ScriptType.P2PK, fullTransaction.outputs[0].scriptType)
        }

        it("extractOutputs_P2SH") {
            assertNull(fullTransaction.outputs[0].keyHash)

            val keyHash = "bd82ef4973ebfcbc8f7cb1d540ef0503a791970b"
            transactionOutput.lockingScript = "A914${keyHash}87".hexToByteArray()
            extractor.extractOutputs(fullTransaction)

            assertEquals(keyHash, fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals(ScriptType.P2SH, fullTransaction.outputs[0].scriptType)
        }

        it("extractOutputs_P2WPKH") {
            assertNull(fullTransaction.outputs[0].keyHash)

            val keyHash = "00148749115073ad59a6f3587f1f9e468adedf01473f".hexToByteArray()
            transactionOutput.lockingScript = keyHash
            extractor.extractOutputs(fullTransaction)

            assertArrayEquals(keyHash, fullTransaction.outputs[0].keyHash)
            assertEquals(ScriptType.P2WPKH, fullTransaction.outputs[0].scriptType)
        }

        //
        // Old e2e tests
        //

        it("extractP2PKH") {
            fullTransaction = Fixtures.transactionP2PKH

            assertNull(fullTransaction.inputs[0].keyHash)
            assertNull(fullTransaction.outputs[0].keyHash)
            assertNull(fullTransaction.outputs[1].keyHash)

            extractor.extractOutputs(fullTransaction)

            // output
            assertEquals(ScriptType.P2PKH, fullTransaction.outputs[0].scriptType)
            assertEquals(ScriptType.P2PKH, fullTransaction.outputs[1].scriptType)
            assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals("37a9bfe84d9e4883ace248509bbf14c9d72af017", fullTransaction.outputs[1].keyHash?.toHexString())

            // // input
            // assertEquals("f6889a22593e9156ef80bdcda0e1b355e8949e05", fullTransaction.inputs[0]?.keyHash?.toHexString())
            // // address
            // assertEquals("n3zWAXKu6LBa8qYGEuTEfg9RXeijRHj5rE", fullTransaction.inputs[0]?.address)
            // assertEquals("mkbGp1uE1jRfdNxtWAUTGWKc9r2pRsLiUi", fullTransaction.outputs[0]?.address)
            // assertEquals("mkbGp1uE1jRfdNxtWAUTGWKc9r2pRsLiUi", fullTransaction.outputs[1]?.address)
        }

        it("extractP2SH") {
            fullTransaction = Fixtures.transactionP2SH

            assertNull(fullTransaction.inputs[0].keyHash)
            assertNull(fullTransaction.outputs[0].keyHash)
            assertNull(fullTransaction.outputs[1].keyHash)

            extractor.extractOutputs(fullTransaction)

            // output
            assertEquals(ScriptType.P2SH, fullTransaction.outputs[0].scriptType)
            assertEquals(ScriptType.P2SH, fullTransaction.outputs[1].scriptType)
            assertEquals("cdfb2eb01489e9fe8bd9b878ce4a7084dd887764", fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals("aed6f804c63da80800892f8fd4cdbad0d3ad6d12", fullTransaction.outputs[1].keyHash?.toHexString())

            // // input
            // assertEquals("aed6f804c63da80800892f8fd4cdbad0d3ad6d12", fullTransaction.inputs[0].keyHash?.toHexString())
            // // address
            // assertEquals("2N9Bh5xXL1CdQohpcqPiphdqtQGuAquWuaG", fullTransaction.inputs[0].address)
            // assertEquals("2NC2MR4p1VsHCgAAo8C5KPmyKhuY6rb6SGN", fullTransaction.outputs[0].address)
            // assertEquals("2N9Bh5xXL1CdQohpcqPiphdqtQGuAquWuaG", fullTransaction.outputs[1].address)
        }

        it("extractP2PK") {
            fullTransaction = Fixtures.transactionP2PK

            assertNull(fullTransaction.inputs[0].keyHash)
            assertNull(fullTransaction.outputs[0].keyHash)
            assertNull(fullTransaction.outputs[1].keyHash)

            extractor.extractOutputs(fullTransaction)

            assertEquals(ScriptType.P2PK, fullTransaction.outputs[0].scriptType)
            assertEquals("04ae1a62fe09c5f51b13905f07f06b99a2f7159b2225f374cd378d71302fa28414e7aab37397f554a7df5f142c21c1b7303b8a0626f1baded5c72a704f7e6cd84c", fullTransaction.outputs[0].keyHash?.toHexString())
            assertEquals("0411db93e1dcdb8a016b49840f8c53bc1eb68a382e97b1482ecad7b148a6909a5cb2e0eaddfb84ccf9744464f82e160bfa9b8b64f9d4c03f999b8643f656b412a3", fullTransaction.outputs[1].keyHash?.toHexString())

            // // address
            // assertEquals("", fullTransaction.inputs[0].address)
            // assertEquals("n4YQoLK25P4RsJ2wJEpKnT6q2WGxt149rs", fullTransaction.outputs[0].address)
            // assertEquals("mh8YhPYEAYs3E7EVyKtB5xrcfMExkkdEMF", fullTransaction.outputs[1].address)
        }
    }
})

