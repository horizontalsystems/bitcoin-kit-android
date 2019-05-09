package io.horizontalsystems.dashkit.core

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.utils.HashUtils

class SingleSha256Hasher : IHasher {
    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.sha256(data)
    }
}