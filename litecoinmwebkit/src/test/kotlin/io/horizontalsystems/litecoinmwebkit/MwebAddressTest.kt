package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vectors from ltcd's address codec for the account of the BIP39 mnemonic
 * "abandon ... about" (go/cmd/vectors).
 */
class MwebAddressTest {

    private val script0 = "021e6fb3b0f0a3efac6f1ec1e2a7cdfc429be04ae8c6ad22899a3585aed3a00a35025bd358721374281095c82ea7c5af2bf16780c5191683785b30730921c3ea47f1"
    private val mainnet0 = "ltcmweb1qqg0xlvas7z37ltr0rmq79f7dl3pfhcz2arr26g5fng6cttkn5q9r2qjm6dv8yym59qgftjpw5lz672l3v7qv2xgksdu9kvrnpysu86j87yec4ejm"
    private val testnet0 = "tmweb1qqg0xlvas7z37ltr0rmq79f7dl3pfhcz2arr26g5fng6cttkn5q9r2qjm6dv8yym59qgftjpw5lz672l3v7qv2xgksdu9kvrnpysu86j87yc35wwv"
    private val script2 = "0349fdd59e043a6123a1e7b64f7cc0bcb256671586715f74e5c04663184c5014dd021ab17284168b3e34c8f2aa949cb874bfcb36dd97cb456f62c884ba58c845b06d"
    private val mainnet2 = "ltcmweb1qqdylm4v7qsaxzgapu7my7lxqhje9vec4sec47a89cprxxxzv2q2d6qs6k9egg95t8c6v3u42jjwtsa9levmdm97tg4hk9jyyhfvvs3dsd5yqndj5"

    @Test
    fun decodeMainnet() {
        val address = MwebAddress.decode(mainnet0)
        assertEquals(MwebAddress.MAINNET_HRP, address.hrp)
        assertEquals(script0, address.scriptPubKey.toHexString())
        assertEquals(script0.substring(0, 66), address.scanPubKey.toHexString())
        assertEquals(script0.substring(66), address.spendPubKey.toHexString())
    }

    @Test
    fun decodeTestnet() {
        val address = MwebAddress.decode(testnet0, MwebAddress.TESTNET_HRP)
        assertEquals(script0, address.scriptPubKey.toHexString())
    }

    @Test
    fun encode() {
        assertEquals(mainnet0, MwebAddress.fromScript(script0.hexToByteArray(), MwebAddress.MAINNET_HRP).stringValue)
        assertEquals(testnet0, MwebAddress.fromScript(script0.hexToByteArray(), MwebAddress.TESTNET_HRP).stringValue)
        assertEquals(mainnet2, MwebAddress.fromScript(script2.hexToByteArray(), MwebAddress.MAINNET_HRP).stringValue)
    }

    @Test
    fun roundTrip() {
        assertEquals(mainnet2, MwebAddress.decode(mainnet2).stringValue)
        assertEquals(mainnet0.uppercase(), MwebAddress.decode(mainnet0.uppercase()).stringValue.uppercase())
    }

    @Test
    fun rejectsWrongNetwork() {
        assertThrows(MwebAddressFormatException::class.java) { MwebAddress.decode(testnet0, MwebAddress.MAINNET_HRP) }
        assertFalse(MwebAddress.isValid(mainnet0, MwebAddress.TESTNET_HRP))
        assertTrue(MwebAddress.isValid(mainnet0, MwebAddress.MAINNET_HRP))
    }

    @Test
    fun rejectsCorruption() {
        val flipped = mainnet0.substring(0, 40) + (if (mainnet0[40] == 'q') 'p' else 'q') + mainnet0.substring(41)
        assertThrows(MwebAddressFormatException::class.java) { MwebAddress.decode(flipped) }
        assertThrows(MwebAddressFormatException::class.java) { MwebAddress.decode(mainnet0.dropLast(1)) }
        assertThrows(MwebAddressFormatException::class.java) { MwebAddress.decode("ltc1qw508d6qejxtdg4y5r3zarvary0c5xw7kgmn4n9") }
        assertThrows(MwebAddressFormatException::class.java) { MwebAddress.decode("LTCMweb1" + mainnet0.substring(8)) }
    }

    @Test
    fun isMwebScript() {
        assertTrue(MwebAddress.isMwebScript(script0.hexToByteArray()))
        assertFalse(MwebAddress.isMwebScript("00140000000000000000000000000000000000000000".hexToByteArray()))
        assertFalse(MwebAddress.isMwebScript(ByteArray(66)))
    }
}
