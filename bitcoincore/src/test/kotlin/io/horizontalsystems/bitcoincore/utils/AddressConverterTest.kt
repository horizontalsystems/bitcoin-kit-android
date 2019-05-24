package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.AddressType
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object AddressConverterTest : Spek({

    lateinit var converter: IAddressConverter
    lateinit var bytes: ByteArray
    lateinit var addressString: String
    lateinit var address: Address

    describe("parse") {

        it("p2pkh") {
            converter = Base58AddressConverter(0, 5)

            bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexToByteArray()
            addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
            address = converter.convert(bytes, ScriptType.P2PKH)

            assertEquals(addressString, address.string)
            assertEquals(AddressType.P2PKH, address.type)

            // TestNet
            converter = Base58AddressConverter(111, 196)

            bytes = "78b316a08647d5b77283e512d3603f1f1c8de68f".hexToByteArray()
            addressString = "mrX9vMRYLfVy1BnZbc5gZjuyaqH3ZW2ZHz"
            address = converter.convert(bytes, ScriptType.P2PKH)

            assertEquals(addressString, address.string)
            assertEquals(AddressType.P2PKH, address.type)

            // Wrong prefix
            assertThrows<AddressFormatException> {
                val testnetAddress = addressString

                converter = Base58AddressConverter(9, 5)
                address = converter.convert(testnetAddress)
            }
        }

        it("p2pkh_cash") {
            converter = CashAddressConverter("bitcoincash")

            // MainNet
            bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexToByteArray()
            addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
            address = converter.convert(bytes, ScriptType.P2PKH)

            assertEquals(addressString, address.string)

            // TestNet
            converter = CashAddressConverter("bchtest")

            bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexToByteArray()
            addressString = "bchtest:pr6m7j9njldwwzlg9v7v53unlr4jkmx6eyvwc0uz5t"
            address = converter.convert(bytes, ScriptType.P2SH)

            assertEquals(addressString, address.string)
        }

        it("p2pkh_cashString") {
            converter = CashAddressConverter("bitcoincash")

            bytes = "f5bf48b397dae70be82b3cca4793f8eb2b6cdac9".hexToByteArray()
            addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
            address = converter.convert(addressString)

            assertArrayEquals(bytes, address.hash)
        }

        it("p2pkh_cashString_withoutPrefix") {
            converter = CashAddressConverter("bitcoincash")

            bytes = "f5bf48b397dae70be82b3cca4793f8eb2b6cdac9".hexToByteArray()
            addressString = "qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
            address = converter.convert(addressString)

            assertArrayEquals(bytes, address.hash)
        }

        it("p2pkh_string") {
            converter = Base58AddressConverter(0, 5)

            bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexToByteArray()
            addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
            address = converter.convert(addressString)

            assertEquals(AddressType.P2PKH, address.type)
            assertArrayEquals(bytes, address.hash)
        }

        it("p2sh") {
            converter = Base58AddressConverter(0, 5)

            bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexToByteArray()
            addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
            address = converter.convert(bytes, ScriptType.P2SH)

            assertEquals(AddressType.P2SH, address.type)
            assertEquals(addressString, address.string)
        }

        it("p2sh_string") {
            converter = Base58AddressConverter(0, 5)

            bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexToByteArray()
            addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
            address = converter.convert(addressString)

            assertEquals(AddressType.P2SH, address.type)
            assertArrayEquals(bytes, address.hash)
        }

        it("p2wpkh") {
            converter = SegwitAddressConverter("bc")

            addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
            bytes = "0014751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()
            address = converter.convert(bytes, ScriptType.P2WPKH)

            assertEquals(AddressType.WITNESS, address.type)
            assertEquals(addressString, address.string)
            assertEquals("751e76e8199196d454941c45d1b3a323f1433bd6", address.hash.toHexString())
        }

        it("p2wpkh_string") {
            converter = SegwitAddressConverter("bc")

            bytes = "751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()
            addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
            address = converter.convert(addressString)

            assertEquals(AddressType.WITNESS, address.type)
            assertEquals(addressString, address.string)
            assertArrayEquals(bytes, address.hash)
        }

    }
})
