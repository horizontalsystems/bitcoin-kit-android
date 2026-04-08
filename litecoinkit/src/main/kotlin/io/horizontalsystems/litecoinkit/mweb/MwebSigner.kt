package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.crypto.schnorr.Schnorr
import io.horizontalsystems.hdwalletkit.ECKey
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Produces per-output spend keys and Schnorr signatures for MWEB transactions.
 *
 * In LIP-0003, for a stealth address output sent to (scanPubKey, spendPubKey):
 *   derivation scalar  t   = SHA-256(scanPrivKey * Ke)  [stored in MwebWalletOutput]
 *   per-output spend key   = spendPrivKey + t  (mod curve order)
 *   commitment blinding r  = spendPrivKey + t  (the blinding factor IS the spend key)
 *
 * NOTE: The input signing message and kernel message constructions below follow the most
 * likely LIP-0003 interpretation. Verify the exact preimages against
 * litecoin-project/litecoin src/mweb/mweb_transact.cpp before production deployment.
 */
class MwebSigner(private val keychain: MwebKeychain) {

    private val curveOrder: BigInteger = ECKey.ecParams.n

    /**
     * Computes the per-output spend private key.
     * Result = (spendPrivKey + derivationScalar) mod n, padded to 32 bytes.
     */
    fun computeOutputSpendKey(derivationScalar: ByteArray): ByteArray {
        val spendPriv = BigInteger(1, keychain.spendPrivKeyBytes)
        val t = BigInteger(1, derivationScalar)
        return toBytes32(spendPriv.add(t).mod(curveOrder))
    }

    /**
     * Computes the kernel excess public key (33-byte compressed EC point).
     * excessPubKey = sum(outputSpendKey_i) * G
     */
    fun computeKernelExcessPubKey(outputSpendKeys: List<ByteArray>): ByteArray {
        val sumScalar = outputSpendKeys
            .map { BigInteger(1, it) }
            .fold(BigInteger.ZERO) { acc, k -> acc.add(k).mod(curveOrder) }
        return ECKey.fromPrivate(toBytes32(sumScalar)).pubKey
    }

    /**
     * Signs an MWEB input with a 64-byte BIP-340 Schnorr signature.
     *
     * @param outputSpendKeyBytes 32-byte per-output spend key from [computeOutputSpendKey]
     * @param message             32-byte message hash
     */
    fun signInput(outputSpendKeyBytes: ByteArray, message: ByteArray): ByteArray {
        require(message.size == 32) { "MWEB input message must be exactly 32 bytes" }
        return Schnorr.sign(message, outputSpendKeyBytes, SecureRandom().generateSeed(32))
    }

    /**
     * Signs an MWEB kernel with a 64-byte BIP-340 Schnorr signature.
     *
     * @param kernelExcessKeyBytes 32-byte sum of all input spend keys (mod n)
     * @param kernelMessage        32-byte kernel message hash
     */
    fun signKernel(kernelExcessKeyBytes: ByteArray, kernelMessage: ByteArray): ByteArray {
        require(kernelMessage.size == 32) { "MWEB kernel message must be exactly 32 bytes" }
        return Schnorr.sign(kernelMessage, kernelExcessKeyBytes, SecureRandom().generateSeed(32))
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
