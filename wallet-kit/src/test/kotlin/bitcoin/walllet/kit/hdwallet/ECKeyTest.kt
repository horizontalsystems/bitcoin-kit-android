package bitcoin.walllet.kit.hdwallet

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.transactions.builder.InputSigner
import helpers.Fixtures
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

class ECKeyTest {

    private lateinit var publicKey: ByteArray
    private lateinit var privateKey: BigInteger
    private lateinit var dataToSign: ByteArray

    private lateinit var ecKey: ECKey

    @Before
    fun setUp() {
        publicKey = "037d56797fbe9aa506fc263751abf23bb46c9770181a6059096808923f0a64cb15".hexStringToByteArray()
        privateKey = BigInteger("4ee8efccaa04495d5d3ab0f847952fcff43ffc0459bd87981b6be485b92f8d64", 16)
        dataToSign = Fixtures.transactionP2PKH_ForSignatureTest.toSignatureByteArray(0) + byteArrayOf(InputSigner.SIGHASH_ALL, 0, 0, 0)
    }

    @Test
    fun createSignature_Success() {

        ecKey = ECKey(publicKey, privateKey, true)

        val expectedSignatureHex = "304402201d914e9d229e4b8cbb7c8dee96f4fdd835cabae7e016e0859c5dc95977b697d50220681395971eecd5df3eb36b8f97f0c8b1a6e98dc7d5662f921e0b2fb0694db0f2"
        val resultSignature = ecKey.createSignature(dataToSign)

        Assert.assertEquals(expectedSignatureHex, resultSignature.toHexString())
    }

    @Test(expected = IllegalStateException::class)
    fun createSignature_NoPrivateKey() {
        ecKey = ECKey(publicKey)

        ecKey.createSignature(dataToSign)
    }

}
