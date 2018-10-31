package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.AddressType
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.network.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.network.TestNet
import io.horizontalsystems.bitcoinkit.network.TestNetBitcoinCash
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressConverterTest {
    private lateinit var converter: AddressConverter
    private lateinit var bytes: ByteArray
    private lateinit var addressString: String
    private lateinit var address: Address

    @Test
    fun p2pkh() {
        converter = AddressConverter(MainNet())

        bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexStringToByteArray()
        addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
        address = converter.convert(bytes, ScriptType.P2PKH)

        assertEquals(addressString, address.string)
        assertEquals(AddressType.P2PKH, address.type)

        // TestNet
        converter = AddressConverter(TestNet())

        bytes = "78b316a08647d5b77283e512d3603f1f1c8de68f".hexStringToByteArray()
        addressString = "mrX9vMRYLfVy1BnZbc5gZjuyaqH3ZW2ZHz"
        address = converter.convert(bytes, ScriptType.P2PKH)

        assertEquals(addressString, address.string)
        assertEquals(AddressType.P2PKH, address.type)
    }

    @Test
    fun p2pkh_cash() {
        converter = AddressConverter(MainNetBitcoinCash())

        bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexStringToByteArray()
        addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
        address = converter.convert(bytes, ScriptType.P2PKH)

        assertEquals(addressString, address.string)

        // TestNet
        converter = AddressConverter(TestNetBitcoinCash())

        bytes = "F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9".hexStringToByteArray()
        addressString = "bchtest:pr6m7j9njldwwzlg9v7v53unlr4jkmx6eyvwc0uz5t"
        address = converter.convert(bytes, ScriptType.P2SH)

        assertEquals(addressString, address.string)
    }

    @Test
    fun p2pkh_cashString() {
        converter = AddressConverter(MainNetBitcoinCash())

        bytes = "f5bf48b397dae70be82b3cca4793f8eb2b6cdac9".hexStringToByteArray()
        addressString = "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2"
        address = converter.convert(addressString)

        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2pkh_string() {
        converter = AddressConverter(MainNet())

        bytes = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexStringToByteArray()
        addressString = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
        address = converter.convert(addressString)

        assertEquals(AddressType.P2PKH, address.type)
        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2sh() {
        converter = AddressConverter(MainNet())

        bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexStringToByteArray()
        addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
        address = converter.convert(bytes, ScriptType.P2SH)

        assertEquals(AddressType.P2SH, address.type)
        assertEquals(addressString, address.string)
    }

    @Test
    fun p2sh_string() {
        converter = AddressConverter(MainNet())

        bytes = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexStringToByteArray()
        addressString = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
        address = converter.convert(addressString)

        assertEquals(AddressType.P2SH, address.type)
        assertArrayEquals(bytes, address.hash)
    }

    @Test
    fun p2wpkh() {
        converter = AddressConverter(MainNet())

        addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        bytes = "0014751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()
        address = converter.convert(bytes, ScriptType.P2WPKH)

        assertEquals(AddressType.WITNESS, address.type)
        assertEquals(addressString, address.string)
        assertEquals("751e76e8199196d454941c45d1b3a323f1433bd6", address.hash.toHexString())
    }

    @Test
    fun p2wpkh_string() {
        converter = AddressConverter(MainNet())

        bytes = "751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()
        addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        address = converter.convert(addressString)

        assertEquals(AddressType.WITNESS, address.type)
        assertEquals(addressString, address.string)
        assertArrayEquals(bytes, address.hash)
    }

}
