package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.exceptions.AddressFormatException
import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.AddressType
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class AddressConverterTest {

    private lateinit var converter: IAddressConverter
    private lateinit var bytes: ByteArray
    private lateinit var addressString: String
    private lateinit var address: Address

    @Test
    fun p2pkh() {
        converter = Base58AddressConverter(0, 5)

        bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexStringToByteArray()
        addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
        address = converter.convert(bytes, ScriptType.P2PKH)

        assertEquals(addressString, address.string)
        assertEquals(AddressType.P2PKH, address.type)

        // TestNet
        converter = Base58AddressConverter(111, 196)

        bytes = "78b316a08647d5b77283e512d3603f1f1c8de68f".hexStringToByteArray()
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

    @Test
    fun p2pkh_cash() {
        converter = CashAddressConverter("bitcoincash")

        // MainNet
        bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexStringToByteArray()
        addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
        address = converter.convert(bytes, ScriptType.P2PKH)

        assertEquals(addressString, address.string)

        // TestNet
        converter = CashAddressConverter("bchtest")

        bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexStringToByteArray()
        addressString = "bchtest:pr6m7j9njldwwzlg9v7v53unlr4jkmx6eyvwc0uz5t"
        address = converter.convert(bytes, ScriptType.P2SH)

        assertEquals(addressString, address.string)
    }

    @Test
    fun p2pkh_cashString() {
        converter = CashAddressConverter("bitcoincash")

        bytes = "f5bf48b397dae70be82b3cca4793f8eb2b6cdac9".hexStringToByteArray()
        addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
        address = converter.convert(addressString)

        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2pkh_cashString_withoutPrefix() {
        converter = CashAddressConverter("bitcoincash")

        bytes = "f5bf48b397dae70be82b3cca4793f8eb2b6cdac9".hexStringToByteArray()
        addressString = "qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
        address = converter.convert(addressString)

        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2pkh_string() {
        converter = Base58AddressConverter(0, 5)

        bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexStringToByteArray()
        addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
        address = converter.convert(addressString)

        assertEquals(AddressType.P2PKH, address.type)
        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2sh() {
        converter = Base58AddressConverter(0, 5)

        bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexStringToByteArray()
        addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
        address = converter.convert(bytes, ScriptType.P2SH)

        assertEquals(AddressType.P2SH, address.type)
        assertEquals(addressString, address.string)
    }

    @Test
    fun p2sh_string() {
        converter = Base58AddressConverter(0, 5)

        bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexStringToByteArray()
        addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
        address = converter.convert(addressString)

        assertEquals(AddressType.P2SH, address.type)
        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2wpkh() {
        converter = SegwitAddressConverter("bc")

        addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        bytes = "0014751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()
        address = converter.convert(bytes, ScriptType.P2WPKH)

        assertEquals(AddressType.WITNESS, address.type)
        assertEquals(addressString, address.string)
        assertEquals("751e76e8199196d454941c45d1b3a323f1433bd6", address.hash.toHexString())
    }

    @Test
    fun p2wpkh_string() {
        converter = SegwitAddressConverter("bc")

        bytes = "751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()
        addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        address = converter.convert(addressString)

        assertEquals(AddressType.WITNESS, address.type)
        assertEquals(addressString, address.string)
        assertArrayEquals(bytes, address.hash)
    }

}
