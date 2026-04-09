package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebRawOutput
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebOutput
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebWalletOutput
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Scans MWEB outputs to detect ownership using pure Kotlin / BouncyCastle.
 * No native library is required for receive detection.
 *
 * Scanning algorithm (LIP-0003 / Litecoin MWEB):
 *  1. ECDH shared secret  S  = scanPrivKey · Ke   (EC scalar multiplication)
 *  2. Derivation scalar   t  = SHA-256(S compressed)
 *  3. Expected one-time key  Ko_exp = spendPubKey + t·G   (EC add + multiply)
 *  4. Output is ours if Ko_exp == Ko (stored in output)
 *  5. Recover value: value = LE64(maskedValue ⊕ t[0..7])
 *
 * NOTE: The exact hash derivation in step 2 and the XOR mask in step 5 match the
 * reference litecoin-project implementation. If scanning produces no results on a
 * known-funded wallet, check the MWEB OutputMessage spec in LIP-0003 for updates.
 */
class MwebScanner(private val keychain: MwebKeychain) {

    private val curve = ECKey.ecParams.curve
    private val G = ECKey.ecParams.g
    private val sha256 = MessageDigest.getInstance("SHA-256")

    data class OwnedOutput(
        val outputId: String,
        val value: Long,
        val raw: MwebRawOutput,
        val derivationScalar: ByteArray  // t = SHA-256(scanPrivKey * Ke); needed to derive spend key
    )

    /** Scans a list of raw outputs; returns only those belonging to this wallet. */
    fun scan(blockHash: String, outputs: List<MwebRawOutput>): Pair<List<MwebOutput>, List<MwebWalletOutput>> {
        val rawEntities = mutableListOf<MwebOutput>()
        val walletEntities = mutableListOf<MwebWalletOutput>()

        for (raw in outputs) {
            val id = raw.commitment.toHex()

            rawEntities.add(
                MwebOutput(
                    outputId = id,
                    commitment = raw.commitment,
                    senderPubKey = raw.senderPubKey,
                    receiverPubKey = raw.receiverPubKey,
                    features = raw.features,
                    maskedValue = raw.maskedValue,
                    maskedNonce = raw.maskedNonce,
                    rangeProofSize = raw.rangeProof.size,
                    rangeProofBytes = raw.rangeProof,
                    leafIndex = raw.leafIndex,
                    blockHash = blockHash
                )
            )

            tryOwn(raw)?.let { owned ->
                walletEntities.add(
                    MwebWalletOutput(
                        outputId = id,
                        value = owned.value,
                        derivationScalar = owned.derivationScalar
                    )
                )
            }
        }

        return rawEntities to walletEntities
    }

    private fun tryOwn(raw: MwebRawOutput): OwnedOutput? = runCatching {
        val Ke = curve.decodePoint(raw.senderPubKey)
        val scanPriv = BigInteger(1, keychain.scanPrivKeyBytes)

        // 1. ECDH: S = scanPrivKey * Ke
        val S = Ke.multiply(scanPriv).normalize()
        val sCompressed = S.getEncoded(true)

        // 2. Derivation scalar t = SHA-256(compressed S)
        val t = digest(sCompressed)

        // 3. Expected Ko = spendPubKey + t*G
        val spendPoint = curve.decodePoint(keychain.spendPubKey)
        val tBig = BigInteger(1, t)
        val Ko_expected = spendPoint.add(G.multiply(tBig)).normalize()

        // 4. Match
        val Ko = curve.decodePoint(raw.receiverPubKey)
        if (!Ko_expected.getEncoded(true).contentEquals(Ko.getEncoded(true))) return null

        // 5. Recover value: LE64(maskedValue XOR t[0..7])
        var value = 0L
        for (i in 0..7) {
            value = value or ((raw.maskedValue[i].toInt() and 0xFF xor (t[i].toInt() and 0xFF)).toLong() shl (i * 8))
        }

        OwnedOutput(raw.commitment.toHex(), value, raw, derivationScalar = t)
    }.getOrNull()

    private fun digest(data: ByteArray): ByteArray = sha256.digest(data)

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}