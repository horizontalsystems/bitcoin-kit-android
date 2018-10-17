package bitcoin.wallet.kit.scripts

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.scripts.ScriptType.P2PKH
import bitcoin.wallet.kit.scripts.ScriptType.P2SH
import bitcoin.wallet.kit.scripts.ScriptType.P2WPKH
import bitcoin.wallet.kit.scripts.ScriptType.P2WSH
import bitcoin.wallet.kit.utils.AddressConverter
import junit.framework.Assert
import org.junit.Test

class ScriptBuilderTest {

    private val scriptBuilder = ScriptBuilder()
    private val networkParameters = MainNet()
    private val addressConverter = AddressConverter(networkParameters)

    @Test
    fun testP2PKH() {
        val address = addressConverter.convert("cbc20a7664f2f69e5355aa427045bc15e7c6c772".hexStringToByteArray(), P2PKH)

        val expectedScript = "76a914cbc20a7664f2f69e5355aa427045bc15e7c6c77288ac"
        val resultScript = scriptBuilder.lockingScript(address)

        Assert.assertEquals(expectedScript, resultScript.toHexString())
    }

    @Test
    fun testP2PSH() {
        val address = addressConverter.convert("2a02dfd19c9108ad48878a01bfe53deaaf30cca4".hexStringToByteArray(), P2SH)

        val expectedScript = "a9142a02dfd19c9108ad48878a01bfe53deaaf30cca487"
        val resultScript = scriptBuilder.lockingScript(address)

        Assert.assertEquals(expectedScript, resultScript.toHexString())
    }

    @Test
    fun testP2WPKH() {
        val address = addressConverter.convert("751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray(), P2WSH)

        val expectedScript = "0014751e76e8199196d454941c45d1b3a323f1433bd6"
        val resultScript = scriptBuilder.lockingScript(address)

        Assert.assertEquals(expectedScript, resultScript.toHexString())
    }

    @Test
    fun testP2WSH() {
        val address = addressConverter.convert("1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262".hexStringToByteArray(), P2WPKH)

        val expectedScript = "00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262"
        val resultScript = scriptBuilder.lockingScript(address)

        Assert.assertEquals(expectedScript, resultScript.toHexString())
    }

    @Test
    fun testP2PKH_UnlockingScript() {
        val pubKeys = listOf(
                "3045022100b78dacbc598d414f29537e33b5e7b209ecde9074b5fb4e68f94e8f5cb88ee9ad02202ef04916e8c1caa8cdb739c9695a51eadeaef6fe8ff7e990cc9031b410a123cc01".hexStringToByteArray(),
                "03ec6877e5c28e459ac4daa3222204e7eef4cb42825b6b43438aeea01dd525b24d".hexStringToByteArray())

        val expectedScript = "483045022100b78dacbc598d414f29537e33b5e7b209ecde9074b5fb4e68f94e8f5cb88ee9ad02202ef04916e8c1caa8cdb739c9695a51eadeaef6fe8ff7e990cc9031b410a123cc012103ec6877e5c28e459ac4daa3222204e7eef4cb42825b6b43438aeea01dd525b24d"
        val resultScript = scriptBuilder.unlockingScript(pubKeys)

        Assert.assertEquals(expectedScript, resultScript.toHexString())
    }

}
