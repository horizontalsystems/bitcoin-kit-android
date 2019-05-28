package io.horizontalsystems.bitcoincore.transactions.scripts

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.AddressType
import io.horizontalsystems.bitcoincore.models.SegWitAddress
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType.P2SH
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ScriptBuilderTest : Spek({

    val scriptBuilder = ScriptBuilder()
    val addressConverter = Base58AddressConverter(0, 5)

    describe("#lockingScript") {
        it("P2PKH") {
            val address = addressConverter.convert("cbc20a7664f2f69e5355aa427045bc15e7c6c772".hexToByteArray(), P2PKH)

            val expectedScript = "76a914cbc20a7664f2f69e5355aa427045bc15e7c6c77288ac"
            val resultScript = scriptBuilder.lockingScript(address)

            assertEquals(expectedScript, resultScript.toHexString())
        }

        it("P2PSH") {
            val address = addressConverter.convert("2a02dfd19c9108ad48878a01bfe53deaaf30cca4".hexToByteArray(), P2SH)

            val expectedScript = "a9142a02dfd19c9108ad48878a01bfe53deaaf30cca487"
            val resultScript = scriptBuilder.lockingScript(address)

            assertEquals(expectedScript, resultScript.toHexString())
        }

        it("P2WPKH") {
            val keyHash = "751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()
            val address = SegWitAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", keyHash, AddressType.WITNESS, 0)

            val expectedScript = "0014751e76e8199196d454941c45d1b3a323f1433bd6"
            val resultScript = scriptBuilder.lockingScript(address)

            assertEquals(expectedScript, resultScript.toHexString())
        }

        it("P2WSH") {
            val keyHash = "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262".hexToByteArray()
            val address = SegWitAddress("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", keyHash, AddressType.WITNESS, 0)

            val expectedScript = "00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262"
            val resultScript = scriptBuilder.lockingScript(address)

            assertEquals(expectedScript, resultScript.toHexString())
        }

        it("P2PKH_UnlockingScript") {
            val pubKeys = listOf(
                    "3045022100b78dacbc598d414f29537e33b5e7b209ecde9074b5fb4e68f94e8f5cb88ee9ad02202ef04916e8c1caa8cdb739c9695a51eadeaef6fe8ff7e990cc9031b410a123cc01".hexToByteArray(),
                    "03ec6877e5c28e459ac4daa3222204e7eef4cb42825b6b43438aeea01dd525b24d".hexToByteArray())

            val expectedScript = "483045022100b78dacbc598d414f29537e33b5e7b209ecde9074b5fb4e68f94e8f5cb88ee9ad02202ef04916e8c1caa8cdb739c9695a51eadeaef6fe8ff7e990cc9031b410a123cc012103ec6877e5c28e459ac4daa3222204e7eef4cb42825b6b43438aeea01dd525b24d"
            val resultScript = scriptBuilder.unlockingScript(pubKeys)

            assertEquals(expectedScript, resultScript.toHexString())
        }
    }
})
