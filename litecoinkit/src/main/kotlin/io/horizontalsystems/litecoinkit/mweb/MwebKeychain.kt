package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.litecoinkit.LitecoinKit

/**
 * Derives the MWEB scan and spend public keys from an HD master key.
 *
 * Derivation paths (BIP32):
 *   Scan key:  m/1'/{coinType}'/0'
 *   Spend key: m/1'/{coinType}'/1'
 *
 * Only master keys (depth = 0) are supported. Account-level keys do not
 * provide enough path context to derive MWEB keys correctly.
 *
 * coinType = 2 for mainnet, 1 for testnet (same as Litecoin BIP44).
 */
class MwebKeychain(extendedKey: HDExtendedKey, networkType: LitecoinKit.NetworkType) {

    val scanPubKey: ByteArray
    val spendPubKey: ByteArray
    /** 32-byte private scalar for the scan key. Required for ECDH output scanning. */
    val scanPrivKeyBytes: ByteArray

    init {
        if (extendedKey.derivedType != HDExtendedKey.DerivedType.Master) {
            throw IllegalArgumentException(
                "MWEB key derivation requires a master (root) HD key, got ${extendedKey.derivedType}"
            )
        }
        val coinType = if (networkType == LitecoinKit.NetworkType.MainNet) 2 else 1
        val keychain = HDKeychain(extendedKey.key)
        val scanHDKey = keychain.getKeyByPath("m/1'/$coinType'/0'")
        scanPubKey = scanHDKey.pubKey
        // privKeyBytes may be < 32 bytes if leading byte is 0; pad to 32
        val rawPriv = scanHDKey.privKeyBytes
        scanPrivKeyBytes = if (rawPriv.size == 32) rawPriv else ByteArray(32 - rawPriv.size) + rawPriv
        spendPubKey = keychain.getKeyByPath("m/1'/$coinType'/1'").pubKey
    }

    companion object {
        /**
         * Returns an [MwebKeychain] if [extendedKey] supports MWEB derivation, or null otherwise.
         */
        fun tryCreate(extendedKey: HDExtendedKey, networkType: LitecoinKit.NetworkType): MwebKeychain? =
            runCatching { MwebKeychain(extendedKey, networkType) }.getOrNull()
    }
}