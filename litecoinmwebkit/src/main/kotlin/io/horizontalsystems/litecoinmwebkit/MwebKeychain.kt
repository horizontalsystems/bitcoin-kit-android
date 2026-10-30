package io.horizontalsystems.litecoinmwebkit

import io.horizontalsystems.hdwalletkit.Curve
import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.Utils

/**
 * The two secrets of an MWEB account. The scan secret recognises incoming
 * outputs (and is all the daemon needs to stream them); the spend secret
 * signs. Addresses are derived from the scan secret and the spend public
 * key, so a view-only keychain can still show addresses.
 *
 * Derivation from a BIP39 seed is `m/1000'/0'` for the scan secret and
 * `m/1000'/1'` for the spend secret, the scheme Cake Wallet uses, so a seed
 * restored here or there finds the same outputs. Litecoin Core's legacy
 * wallet derives `m/0'/100'/{0',1'}` from its own non-BIP39 HD seed and
 * cannot be restored from a mnemonic in the first place.
 */
class MwebKeychain(val scanSecret: ByteArray, val spendSecret: ByteArray?, spendPubKey: ByteArray? = null) {

    init {
        require(scanSecret.size == 32) { "scan secret must be 32 bytes" }
        require(spendSecret == null || spendSecret.size == 32) { "spend secret must be 32 bytes" }
        require(spendSecret != null || spendPubKey != null) { "spend secret or spend public key required" }
    }

    /** Compressed spend public key, 33 bytes. */
    val spendPubKey: ByteArray = spendPubKey
        ?: ECKey.pubKeyFromPrivKey(java.math.BigInteger(1, spendSecret), true)

    companion object {
        const val SCAN_PATH = "m/1000'/0'"
        const val SPEND_PATH = "m/1000'/1'"

        fun fromSeed(seed: ByteArray): MwebKeychain {
            val keychain = HDKeychain(seed, Curve.Secp256K1)
            val scan = Utils.bigIntegerToBytes(keychain.getKeyByPath(SCAN_PATH).privKey, 32)
            val spend = Utils.bigIntegerToBytes(keychain.getKeyByPath(SPEND_PATH).privKey, 32)
            return MwebKeychain(scan, spend)
        }

        fun fromWords(words: List<String>, passphrase: String): MwebKeychain =
            fromSeed(Mnemonic().toSeed(words, passphrase))
    }
}
