//package io.horizontalsystems.bitcoinkit.transactions.builder
//
//import com.nhaarman.mockito_kotlin.any
//import com.nhaarman.mockito_kotlin.whenever
//import helpers.Fixtures
//import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
//import io.horizontalsystems.bitcoinkit.core.toHexString
//import io.horizontalsystems.bitcoinkit.network.Network
//import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
//import io.horizontalsystems.hdwalletkit.HDKey
//import io.horizontalsystems.hdwalletkit.HDWallet
//import org.junit.Assert.assertEquals
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.anyBoolean
//import org.mockito.Mockito.mock
//
//class InputSignerTest {
//
//    private val network = mock(Network::class.java)
//    private val hdWallet = mock(HDWallet::class.java)
//    private val privateKey = mock(HDKey::class.java)
//
//    private var derEncodedSignature = "abc".hexStringToByteArray()
//    private var transaction = Fixtures.transactionP2PKH_ForSignatureTest
//
//    private lateinit var inputSigner: InputSigner
//
//    @Before
//    fun setUp() {
//        whenever(privateKey.createSignature(any())).thenReturn(derEncodedSignature)
//        whenever(hdWallet.privateKey(any(), any(), anyBoolean())).thenReturn(privateKey)
//
//        inputSigner = InputSigner(hdWallet, network)
//    }
//
//    @Test(expected = InputSigner.Error.NoPreviousOutput::class)
//    fun sigScriptData_NoInputAtIndex() {
//        transaction.inputs[0] = null
//
//        inputSigner.sigScriptData(transaction, 0)
//    }
//
//    @Test(expected = InputSigner.Error.NoPreviousOutput::class)
//    fun sigScriptData_NoPreviousOutput() {
//        transaction.inputs[0]?.previousOutput = null
//
//        inputSigner.sigScriptData(transaction, 0)
//    }
//
//    @Test(expected = InputSigner.Error.NoPreviousOutputAddress::class)
//    fun sigScriptData_NoPreviousOutputAddress() {
//        transaction.inputs[0]?.previousOutput?.publicKey = null
//
//        inputSigner.sigScriptData(transaction, 0)
//    }
//
//    @Test(expected = InputSigner.Error.NoPrivateKey::class)
//    fun sigScriptData_NoPrivateKey() {
//        whenever(hdWallet.privateKey(any(), any(), anyBoolean())).thenReturn(null)
//
//        inputSigner.sigScriptData(transaction, 0)
//    }
//
//    @Test
//    fun sigScriptData_CorrectSignature() {
//        val previousOutputPubKey = transaction.inputs[0]?.previousOutput?.publicKey
//
//        val resultSignature = inputSigner.sigScriptData(transaction, 0)
//        val expectedSignature = derEncodedSignature.toHexString() + "01"
//
//        assertEquals(2, resultSignature.size)
//        assertEquals(expectedSignature, resultSignature[0].toHexString())
//        assertEquals(previousOutputPubKey?.publicKey, resultSignature[1])
//    }
//
//    @Test
//    fun sigScriptData_P2PK() {
//        transaction.inputs[0]?.previousOutput.apply {
//            this?.scriptType = ScriptType.P2PK
//        }
//
//        val resultSignature = inputSigner.sigScriptData(transaction, 0)
//        val expectedSignature = derEncodedSignature.toHexString() + "01"
//
//        assertEquals(1, resultSignature.size)
//        assertEquals(expectedSignature, resultSignature[0].toHexString())
//    }
//
////    @Test
////    fun sigScriptData_P2WPKH() {
////
////        transaction.inputs[0]?.previousOutput.apply {
////            this?.scriptType = ScriptType.P2WPKH
////        }
////
////        val resultSignature = inputSigner.sigScriptData(transaction, 0)
////        val expectedSignature = derEncodedSignature.toHexString() + "01"
////
////        assertEquals(2, resultSignature.size)
////        assertEquals(expectedSignature, resultSignature[0].toHexString())
////    }
//
//}
