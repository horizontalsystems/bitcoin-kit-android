package bitcoin.wallet.kit.utils

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.hdwallet.AddressType
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.scripts.ScriptType
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

        assertEquals(AddressType.P2PKH, address.type)
        assertEquals(addressString, address.string)
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
        bytes = "751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()
        address = converter.convert(bytes, ScriptType.P2WPKH)

        assertEquals(AddressType.WITNESS, address.type)
        assertEquals(addressString, address.string)
        assertEquals(bytes, address.hash)
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
