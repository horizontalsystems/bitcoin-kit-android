package bitcoin.wallet.kit.transactions.builder

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.walllet.kit.hdwallet.HDKey
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class InputSignerTest {

    private val hdWallet = mock(HDWallet::class.java)
    private val privateKey = mock(HDKey::class.java)
    private val inputSigner = InputSigner(hdWallet)

    private var transaction = Fixtures.transactionP2PKH_ForSignatureTest

    @Before
    fun setUp() {

        whenever(hdWallet.privateKey(any(), any())).thenReturn(privateKey)

        val serializedTransaction = transaction.toSignatureByteArray(0) + byteArrayOf(InputSigner.SIGHASH_ALL, 0, 0, 0)
        val derEncodedSignature = "304402201d914e9d229e4b8cbb7c8dee96f4fdd835cabae7e016e0859c5dc95977b697d50220681395971eecd5df3eb36b8f97f0c8b1a6e98dc7d5662f921e0b2fb0694db0f2".hexStringToByteArray()

        whenever(privateKey.createSignature(serializedTransaction)).thenReturn(derEncodedSignature)
    }

    @Test
    fun sigScriptData_CorrectSignature() {
        val previousOutputPubKey = transaction.inputs[0]?.previousOutput?.publicKey
        val expectedSignature = "304402201d914e9d229e4b8cbb7c8dee96f4fdd835cabae7e016e0859c5dc95977b697d50220681395971eecd5df3eb36b8f97f0c8b1a6e98dc7d5662f921e0b2fb0694db0f201"

        val resultSignature = inputSigner.sigScriptData(transaction, 0)

        Assert.assertEquals(2, resultSignature.size)
        Assert.assertEquals(expectedSignature, resultSignature[0].toHexString())
        Assert.assertEquals(previousOutputPubKey?.publicKey, resultSignature[1])
    }

    @Test(expected = InputSigner.NoInputAtIndexException::class)
    fun sigScriptData_NoInputAtIndex() {
        transaction.inputs[0] = null

        inputSigner.sigScriptData(transaction, 0)
    }

    @Test(expected = InputSigner.NoPreviousOutputException::class)
    fun sigScriptData_NoPreviousOutput() {
        transaction.inputs[0]?.previousOutput = null

        inputSigner.sigScriptData(transaction, 0)
    }

    @Test(expected = InputSigner.NoPreviousOutputAddressException::class)
    fun sigScriptData_NoPreviousOutputAddress() {
        transaction.inputs[0]?.previousOutput?.publicKey = null

        inputSigner.sigScriptData(transaction, 0)
    }

    @Test(expected = InputSigner.NoPrivateKeyException::class)
    fun sigScriptData_NoPrivateKey() {
        whenever(hdWallet.privateKey(any(), any())).thenReturn(null)

        inputSigner.sigScriptData(transaction, 0)
    }

    @Test
    fun sigScriptData_TransactionP2PK() {
        val expectedSignature = "304402201d914e9d229e4b8cbb7c8dee96f4fdd835cabae7e016e0859c5dc95977b697d50220681395971eecd5df3eb36b8f97f0c8b1a6e98dc7d5662f921e0b2fb0694db0f201"

        val inputPrevOutput = transaction.inputs[0]?.previousOutput
        inputPrevOutput?.scriptType = ScriptType.P2PK

        val resultSignature = inputSigner.sigScriptData(transaction, 0)

        Assert.assertEquals(1, resultSignature.size)
        Assert.assertEquals(expectedSignature, resultSignature[0].toHexString())
    }

}
