package io.horizontalsystems.litecoinkit.mweb

import com.appmattus.crypto.Algorithm

/**
 * BLAKE3 helpers for MWEB protocol operations.
 *
 * MWEB uses plain (unkeyed) BLAKE3 with 32-byte output for:
 *  - output_id = BLAKE3(serialized_output)
 *  - kernel signature message = BLAKE3(features || excess_commitment || fee || pegouts)
 *  - input aggregation hash = BLAKE3(K_i || K_o)
 */
object MwebHash {

    fun blake3(data: ByteArray): ByteArray {
        val digest = Algorithm.Blake3(32).createDigest()
        digest.update(data)
        return digest.digest()
    }

    fun blake3(vararg parts: ByteArray): ByteArray = blake3(parts.reduce { a, b -> a + b })
}
