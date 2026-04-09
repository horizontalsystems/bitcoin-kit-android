package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.crypto.schnorr.Schnorr
import io.horizontalsystems.hdwalletkit.ECKey
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Produces per-output spend keys and Schnorr signatures for MWEB peg-out transactions.
 *
 * Key relationships (LIP-0003):
 *   k_o   = spendPrivKey + t  (mod n)      — per-output spend key / commitment blinding factor
 *   K_o   = k_o * G                        — one-time output pubkey (= receiverPubKey stored in DB)
 *   k_exc = -(sum of k_o_i)  (mod n)       — kernel excess scalar (negated for peg-out)
 *   K_exc = k_exc * G                      — kernel excess Pedersen commitment (33-byte compressed point)
 *
 * Input aggregated signing key (one per UTXO being spent):
 *   k_agg = k_o + BLAKE3(K_exc || K_o) * k_exc   (mod n)
 *   K_agg = k_agg * G                             — inputPubKey field in wire format
 */
class MwebSigner(private val keychain: MwebKeychain) {

    private val curveOrder: BigInteger = ECKey.ecParams.n

    /** k_o = (spendPrivKey + t) mod n */
    fun computeOutputSpendKey(derivationScalar: ByteArray): ByteArray {
        val spendPriv = BigInteger(1, keychain.spendPrivKeyBytes)
        val t = BigInteger(1, derivationScalar)
        return toBytes32(spendPriv.add(t).mod(curveOrder))
    }

    /**
     * Kernel excess scalar = -(sum of k_o_i) mod n.
     * For peg-out with no MWEB change, the excess equals the negation of all input blinding factors.
     */
    fun computeKernelExcessScalar(outputSpendKeys: List<ByteArray>): ByteArray {
        val sum = outputSpendKeys
            .map { BigInteger(1, it) }
            .fold(BigInteger.ZERO) { acc, k -> acc.add(k).mod(curveOrder) }
        return toBytes32(curveOrder.subtract(sum).mod(curveOrder))
    }

    /**
     * Kernel excess commitment = excessScalar * G (33-byte compressed EC point).
     * This is a Pedersen commitment with value=0 and blinding=excessScalar.
     */
    fun computeKernelExcessCommitment(excessScalar: ByteArray): ByteArray =
        ECKey.fromPrivate(excessScalar).pubKey

    /**
     * Aggregated input signing key for one UTXO:
     *   k_agg = k_o + BLAKE3(K_exc || K_o) * k_exc   (mod n)
     *
     * @param k_o   per-output spend key (32 bytes)
     * @param K_o   one-time output pubkey = receiverPubKey (33 bytes)
     * @param k_exc kernel excess scalar (32 bytes)
     * @param K_exc kernel excess commitment (33 bytes)
     */
    fun computeAggregatedSigningKey(
        k_o: ByteArray,
        K_o: ByteArray,
        k_exc: ByteArray,
        K_exc: ByteArray
    ): ByteArray {
        val h = BigInteger(1, MwebHash.blake3(K_exc + K_o))
        val ko = BigInteger(1, k_o)
        val ke = BigInteger(1, k_exc)
        val k_agg = ko.add(h.multiply(ke)).mod(curveOrder)
        return toBytes32(k_agg)
    }

    /**
     * Signs an MWEB input. The message is the 32-byte output_id (BLAKE3 hash of the output).
     * Uses the aggregated signing key from [computeAggregatedSigningKey].
     */
    fun signInput(k_agg: ByteArray, outputId: ByteArray): ByteArray {
        require(outputId.size == 32) { "output_id must be 32 bytes, got ${outputId.size}" }
        return Schnorr.sign(outputId, k_agg, SecureRandom().generateSeed(32))
    }

    /**
     * Signs an MWEB kernel. The message is the 32-byte BLAKE3 kernel signature message.
     * Uses the kernel excess scalar from [computeKernelExcessScalar].
     */
    fun signKernel(excessScalar: ByteArray, kernelMessage: ByteArray): ByteArray {
        require(kernelMessage.size == 32) { "kernel message must be 32 bytes, got ${kernelMessage.size}" }
        return Schnorr.sign(kernelMessage, excessScalar, SecureRandom().generateSeed(32))
    }

    private fun toBytes32(n: BigInteger): ByteArray {
        val bytes = n.toByteArray()
        return when {
            bytes.size == 32 -> bytes
            bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.copyOfRange(1, 33)
            bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
            else -> throw IllegalStateException("Scalar out of range: ${bytes.size} bytes")
        }
    }
}
