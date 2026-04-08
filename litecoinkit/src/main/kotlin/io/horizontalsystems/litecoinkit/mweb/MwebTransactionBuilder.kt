package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebInput
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebKernel
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebTx
import io.horizontalsystems.litecoinkit.mweb.storage.MwebDao
import io.horizontalsystems.litecoinkit.mweb.storage.MwebStorage
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Constructs signed MWEB peg-out transactions.
 *
 * A peg-out converts MWEB coins to canonical Litecoin by:
 *   1. Spending one or more MWEB UTXOs (inputs)
 *   2. Creating a peg-out kernel that specifies the destination canonical scriptPubKey
 *   3. Producing zero new MWEB outputs (no change — see limitation below)
 *
 * **MVP Limitation**: Change is not supported. If the selected UTXOs exceed
 * `sendAmount + fee`, an exception is thrown. The caller must select an exact
 * combination of UTXOs or use `send-all` semantics. Generating MWEB change
 * requires Bulletproof+ range proof creation, which is not yet implemented.
 *
 * **Signing caveats**: The exact preimages for the input message and kernel message
 * hashes follow the most likely LIP-0003 interpretation. These MUST be verified
 * against litecoin-project/litecoin src/mweb/mweb_transact.cpp before production use.
 */
class MwebTransactionBuilder(
    private val storage: MwebStorage,
    private val signer: MwebSigner
) {

    /**
     * Builds and signs a peg-out [MwebTx].
     *
     * @param pegOutScript scriptPubKey of the destination canonical address
     * @param sendAmount   amount to send in satoshis (must not include fee)
     * @param fee          fee in satoshis
     * @throws IllegalArgumentException if MWEB balance is insufficient
     * @throws IllegalStateException    if selected UTXOs total != sendAmount + fee (change not supported)
     */
    fun buildPegOut(pegOutScript: ByteArray, sendAmount: Long, fee: Long): MwebTx {
        val totalNeeded = sendAmount + fee

        val allSpendable = storage.getSpendableOutputs()
        val selected = selectCoins(allSpendable, totalNeeded)
        val selectedTotal = selected.sumOf { it.value }

        require(selectedTotal >= totalNeeded) {
            "Insufficient MWEB balance: have $selectedTotal sat, need $totalNeeded sat"
        }
        check(selectedTotal == totalNeeded) {
            "MWEB peg-out with change is not yet supported. " +
                "Selected ${selected.size} output(s) totalling $selectedTotal sat, " +
                "but only $totalNeeded sat needed. " +
                "Please select exact UTXOs or adjust the fee."
        }

        // Per-output spend keys: spendPrivKey + t (mod n)
        val outputSpendKeys = selected.map { signer.computeOutputSpendKey(it.derivationScalar) }

        // Kernel excess = sum of all input spend keys (no MWEB outputs to subtract)
        val kernelExcessPubKey = signer.computeKernelExcessPubKey(outputSpendKeys)
        val kernelExcessKey = sumScalars(outputSpendKeys)

        // Build and sign the kernel
        val kernelFeatures = MwebKernel.FEAT_PEGGED_OUT
        val kernelMessage = buildKernelMessage(kernelFeatures, fee, sendAmount, pegOutScript)
        val kernelSig = signer.signKernel(kernelExcessKey, kernelMessage)

        val kernel = MwebKernel(
            features = kernelFeatures,
            fee = fee,
            pegOutAmount = sendAmount,
            pegOutScript = pegOutScript,
            excessPubKey = kernelExcessPubKey,
            signature = kernelSig
        )

        // Build and sign each input
        val inputs = selected.mapIndexed { i, output ->
            val inputMessage = buildInputMessage(kernelExcessPubKey, output.commitment)
            val inputSig = signer.signInput(outputSpendKeys[i], inputMessage)
            MwebInput(
                features = 0x00,
                outputId = output.commitment,
                commitment = output.commitment,
                inputPubKey = output.receiverPubKey,  // Ko — one-time spend pubkey
                signature = inputSig
            )
        }

        return MwebTx(inputs = inputs, kernels = listOf(kernel))
    }

    /**
     * Estimates the fee for a peg-out transaction in satoshis.
     *
     * Approximate sizes:
     *   Per input:  1 + 33 + 33 + 33 + 64 = 164 bytes
     *   Kernel:     1 + 8 + 8 + varint + scriptLen + 33 + 64 ≈ 155 + scriptLen bytes
     */
    fun estimateFee(sendAmount: Long, feeRate: Int): Long {
        val allSpendable = storage.getSpendableOutputs()
        val selected = selectCoins(allSpendable, sendAmount)
        val scriptLen = 22  // typical P2WPKH scriptPubKey length
        val txSize = selected.size * 164L + 155L + scriptLen
        return txSize * feeRate
    }

    // Greedy ascending coin selection: smallest first until total >= target
    private fun selectCoins(
        available: List<MwebDao.SpendableOutput>,
        target: Long
    ): List<MwebDao.SpendableOutput> {
        val result = mutableListOf<MwebDao.SpendableOutput>()
        var sum = 0L
        for (output in available) {  // already sorted ASC by DAO query
            if (sum >= target) break
            result.add(output)
            sum += output.value
        }
        return result
    }

    /**
     * Kernel message hash.
     *
     * Preimage: features(1) || fee_LE64(8) || pegOutAmount_LE64(8) || varint(scriptLen) || script
     * Hash: double-SHA256(preimage)
     *
     * CAVEAT: Verify exact construction against litecoin-project source before shipping.
     */
    private fun buildKernelMessage(
        features: Byte,
        fee: Long,
        pegOutAmount: Long,
        pegOutScript: ByteArray
    ): ByteArray {
        val preimage = BitcoinOutput()
            .writeByte(features.toInt())
            .writeLong(fee)
            .writeLong(pegOutAmount)
            .writeVarInt(pegOutScript.size.toLong())
            .write(pegOutScript)
            .toByteArray()
        return doubleSha256(preimage)
    }

    /**
     * Input signing message hash.
     *
     * Preimage: kernelExcessPubKey(33) || commitment(33)
     * Hash: SHA-256(preimage)
     *
     * CAVEAT: Verify exact construction against litecoin-project source before shipping.
     */
    private fun buildInputMessage(kernelExcessPubKey: ByteArray, commitment: ByteArray): ByteArray {
        return sha256(kernelExcessPubKey + commitment)
    }

    private fun sumScalars(keys: List<ByteArray>): ByteArray {
        val n = io.horizontalsystems.hdwalletkit.ECKey.ecParams.n
        val sum = keys.map { BigInteger(1, it) }.fold(BigInteger.ZERO) { acc, k -> acc.add(k).mod(n) }
        val bytes = sum.toByteArray()
        return when {
            bytes.size == 32 -> bytes
            bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.copyOfRange(1, 33)
            bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
            else -> throw IllegalStateException("Scalar overflow")
        }
    }

    private fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    private fun doubleSha256(data: ByteArray): ByteArray = sha256(sha256(data))
}
