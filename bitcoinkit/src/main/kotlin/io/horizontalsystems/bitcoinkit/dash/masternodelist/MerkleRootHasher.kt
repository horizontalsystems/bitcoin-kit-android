package io.horizontalsystems.bitcoinkit.dash.masternodelist

import io.horizontalsystems.bitcoinkit.dash.IHasher
import io.horizontalsystems.bitcoinkit.dash.IMerkleHasher
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class MerkleRootHasher: IHasher, IMerkleHasher {

    override fun hash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }

    override fun hash(first: ByteArray, second: ByteArray): ByteArray {
        return HashUtils.doubleSha256(first + second)
    }
}
