package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The derivation must match Cake Wallet (`m/1000'` then hardened children
 * 0 and 1 from the BIP39 seed) so a seed restores the same account in both
 * wallets. Expected values from ltcd's hdkeychain (go/cmd/vectors).
 */
class MwebKeychainTest {

    private val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about".split(" ")
    private val seed = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4"

    @Test
    fun derivesScanAndSpendSecrets() {
        val keychain = MwebKeychain.fromSeed(seed.hexToByteArray())
        assertEquals("e1520f2a744b77b15fab8f476241543328499a2ae88caeb5b65110caa8a73959", keychain.scanSecret.toHexString())
        assertEquals("ed8edcdab8e485bc0342e4c01441b759e8b5d5a9641cafe65cba63af09576021", keychain.spendSecret!!.toHexString())
        assertEquals("0265ea63e132fa775931af5e5e8d03eb0a851d4a406d9bab18f6a2e1155df5698e", keychain.spendPubKey.toHexString())
    }

    @Test
    fun derivesFromWords() {
        val keychain = MwebKeychain.fromWords(words, "")
        assertEquals("e1520f2a744b77b15fab8f476241543328499a2ae88caeb5b65110caa8a73959", keychain.scanSecret.toHexString())
    }

    @Test
    fun viewOnlyKeychainKeepsSpendPubKey() {
        val full = MwebKeychain.fromSeed(seed.hexToByteArray())
        val viewOnly = MwebKeychain(full.scanSecret, null, full.spendPubKey)
        assertEquals(full.spendPubKey.toHexString(), viewOnly.spendPubKey.toHexString())
    }
}
