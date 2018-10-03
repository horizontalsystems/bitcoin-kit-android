package bitcoin.walllet.kit.hdwallet

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.TestNet
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressTest {
    val mainNet = MainNet()
    val testNet = TestNet()

    private lateinit var text: String
    private lateinit var hash: ByteArray
    private lateinit var addr: Address

    @Test
    fun p2pkh_mainnet() {
        hash = "e34cce70c86373273efcc54ce7d2a491bb4a0e84".hexStringToByteArray()
        addr = Address(Address.Type.P2PKH, hash, mainNet)

        assertEquals("1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX", addr.toString())
    }

    @Test
    fun p2pkh_mainnet_string() {
        text = "1MirQ9bwyQcGVJPwKUgapu5ouK2E2Ey4gX"
        addr = Address(text, mainNet)

        assertEquals(Address.Type.P2PKH, addr.type)
        assertEquals("e34cce70c86373273efcc54ce7d2a491bb4a0e84", addr.hash.toHexString())
    }

    @Test
    fun p2pkh_testnet() {
        hash = "78b316a08647d5b77283e512d3603f1f1c8de68f".hexStringToByteArray()
        addr = Address(Address.Type.P2PKH, hash, testNet)

        assertEquals(Address.Type.P2PKH, addr.type)
        assertEquals("mrX9vMRYLfVy1BnZbc5gZjuyaqH3ZW2ZHz", addr.toString())
    }

    @Test
    fun p2sh_mainnet() {
        hash = "f815b036d9bbbce5e9f2a00abd1bf3dc91e95510".hexStringToByteArray()
        addr = Address(Address.Type.P2SH, hash, mainNet)

        assertEquals(Address.Type.P2SH, addr.type)
        assertEquals("3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC", addr.toString())
    }

    @Test
    fun p2sh_mainnet_string() {
        text = "3QJmV3qfvL9SuYo34YihAf3sRCW3qSinyC"
        addr = Address(text, mainNet)

        assertEquals(Address.Type.P2SH, addr.type)
        assertEquals("f815b036d9bbbce5e9f2a00abd1bf3dc91e95510", addr.hash.toHexString())
    }

    @Test
    fun p2sh_testnet() {
        hash = "c579342c2c4c9220205e2cdc285617040c924a0a".hexStringToByteArray()
        addr = Address(Address.Type.P2SH, hash, testNet)

        assertEquals("2NBFNJTktNa7GZusGbDbGKRZTxdK9VVez3n", addr.toString())
    }

    @Test
    fun p2wpkh() {
        text = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        hash = "751e76e8199196d454941c45d1b3a323f1433bd6".hexStringToByteArray()

        addr = Address(Address.Type.WITNESS, hash, mainNet)

        assertEquals(Address.Type.WITNESS, addr.type)
        assertEquals(text, addr.toString())
    }

    @Test
    fun p2wpkh_string() {
        text = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        addr = Address(text, mainNet)

        assertEquals(Address.Type.WITNESS, addr.type)
        assertEquals(text, addr.toString())
    }

    @Test
    fun p2wsh() {
        text = "bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3"
        hash = "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262".hexStringToByteArray()

        addr = Address(Address.Type.WITNESS, hash, mainNet)

        assertEquals(Address.Type.WITNESS, addr.type)
        assertEquals(text, addr.toString())
    }

    @Test
    fun p2wsh_string() {
        text = "bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3"
        addr = Address(text, mainNet)

        assertEquals(Address.Type.WITNESS, addr.type)
        assertEquals(text, addr.toString())
    }
}
