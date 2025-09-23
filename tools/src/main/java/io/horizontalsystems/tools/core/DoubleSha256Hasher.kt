package io.horizontalsystems.tools.core

import io.horizontalsystems.tools.utils.HashUtils


class DoubleSha256Hasher : IHasher {
    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }
}
