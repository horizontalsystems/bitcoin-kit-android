package io.horizontalsystems.bitcoincore.transactions.builder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.hdwalletkit.HDKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object InputSignerTest : Spek({

    lateinit var inputSigner: InputSigner

    val publicKey = mock(PublicKey::class.java)
    val inputToSign = mock(InputToSign::class.java)
    val transactionOutput = mock(TransactionOutput::class.java)
    val transactionInput = mock(TransactionInput::class.java)
    val transaction = mock(Transaction::class.java)

    val network = mock(Network::class.java)
    val hdWallet = mock(HDWallet::class.java)
    val privateKey = mock(HDKey::class.java)

    val derEncodedSignature = "abc".hexToByteArray()

    beforeEachTest {
        whenever(inputToSign.previousOutputPublicKey).thenReturn(publicKey)

        whenever(publicKey.publicKey).thenReturn(byteArrayOf(1, 2, 3))
        whenever(privateKey.createSignature(any())).thenReturn(derEncodedSignature)
        whenever(hdWallet.privateKey(any(), any(), anyBoolean())).thenReturn(privateKey)
        whenever(network.sigHashForked).thenReturn(false)
        whenever(network.sigHashValue).thenReturn(Sighash.ALL)

        inputSigner = InputSigner(hdWallet, network)
    }

    describe("when no private key") {
        beforeEach {
            whenever(hdWallet.privateKey(any(), any(), anyBoolean())).thenReturn(null)
        }

        it("throws an exception NoPrivateKey") {
            assertThrows<InputSigner.Error.NoPrivateKey> {
                inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(transactionOutput), 0)
            }
        }
    }

    describe("when private key exist") {
        val lockingScript = "76a914e4de5d630c5cacd7af96418a8f35c411c8ff3c0688ac".hexToByteArray()
        val expectedSignature = derEncodedSignature.toHexString() + "01"

        beforeEach {
            whenever(hdWallet.privateKey(any(), any(), anyBoolean())).thenReturn(privateKey)

            whenever(transactionOutput.lockingScript).thenReturn(lockingScript)
            whenever(transactionOutput.transactionHash).thenReturn(byteArrayOf(1, 2, 3))
            whenever(transactionOutput.scriptType).thenReturn(ScriptType.P2PKH)

            whenever(inputToSign.previousOutput).thenReturn(transactionOutput)
            whenever(inputToSign.input).thenReturn(transactionInput)
        }

        it("signs data") {
            val resultSignature = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(transactionOutput), 0)

            assertEquals(2, resultSignature.size)
            assertEquals(expectedSignature, resultSignature[0].toHexString())
            assertEquals(inputToSign.previousOutputPublicKey.publicKey, resultSignature[1])
        }

        it("signs P2PK") {
            whenever(transactionOutput.scriptType).thenReturn(ScriptType.P2PK)
            val resultSignature = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(transactionOutput), 0)

            assertEquals(1, resultSignature.size)
            assertEquals(expectedSignature, resultSignature[0].toHexString())
        }

        it("signs P2WPKH") {
            whenever(transactionOutput.scriptType).thenReturn(ScriptType.P2WPKH)
            whenever(transactionOutput.keyHash).thenReturn(byteArrayOf(1, 2, 3))

            val resultSignature = inputSigner.sigScriptData(transaction, listOf(inputToSign), listOf(transactionOutput), 0)

            assertEquals(2, resultSignature.size)
            assertEquals(expectedSignature, resultSignature[0].toHexString())
            assertEquals(inputToSign.previousOutputPublicKey.publicKey, resultSignature[1])
        }
    }
})
