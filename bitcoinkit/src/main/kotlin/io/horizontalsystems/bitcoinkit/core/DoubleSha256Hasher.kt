package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.utils.HashUtils

class DoubleSha256Hasher : IHasher {
    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }
}
