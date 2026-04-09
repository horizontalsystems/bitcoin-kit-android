package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebInput
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebKernel
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebTx
import io.horizontalsystems.litecoinkit.mweb.storage.MwebDao
import io.horizontalsystems.litecoinkit.mweb.storage.MwebStorage

/**
 * Constructs signed MWEB peg-out transactions.
 *
 * A peg-out converts MWEB coins to canonical Litecoin by:
 *   1. Spending one or more MWEB UTXOs (inputs)
 *   2. Creating a peg-out kernel specifying the destination canonical scriptPubKey
 *   3. Producing zero new MWEB outputs (no change — see limitation below)
 *
 * Cryptographic operations are delegated to [MwebSigner]. Hash operations use BLAKE3 via [MwebHash].
 *
 * **MVP limitation**: Change is not supported. Selected UTXOs must total exactly `sendAmount + fee`.
 * Generating MWEB change requires Bulletproof+ range proof creation (out of scope).
 */
class MwebTransactionBuilder(
    private val storage: MwebStorage,
    private val signer: MwebSigner
) {

    /**
     * Builds and signs a peg-out [MwebTx].
     *
     * @param pegOutScript scriptPubKey of the destination canonical address
     * @param sendAmount   amount in satoshis (exclusive of fee)
     * @param fee          fee in satoshis
     */
    data class PegOutResult(val tx: MwebTx, val spentDbOutputIds: List<String>)

    fun buildPegOut(pegOutScript: ByteArray, sendAmount: Long, fee: Long): PegOutResult {
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
                "but only $totalNeeded sat needed. Adjust fee or UTXO selection."
        }

        // Per-output spend keys: k_o_i = spendPrivKey + t_i (mod n)
        val outputSpendKeys = selected.map { signer.computeOutputSpendKey(it.derivationScalar) }

        // Kernel excess = -(sum of k_o_i) mod n
        val kernelExcessScalar = signer.computeKernelExcessScalar(outputSpendKeys)
        val kernelExcessCommitment = signer.computeKernelExcessCommitment(kernelExcessScalar)

        // Build and sign the kernel
        val kernelFeatures = MwebKernel.FEAT_PEGOUT_WITH_FEE
        val kernelMessage = buildKernelMessage(kernelFeatures, fee, sendAmount, pegOutScript, kernelExcessCommitment)
        val kernelSig = signer.signKernel(kernelExcessScalar, kernelMessage)

        val kernel = MwebKernel(
            features = kernelFeatures,
            fee = fee,
            pegOutAmount = sendAmount,
            pegOutScript = pegOutScript,
            excessCommitment = kernelExcessCommitment,
            signature = kernelSig
        )

        // Build and sign each input
        val inputs = selected.mapIndexed { i, output ->
            val k_o = outputSpendKeys[i]
            val K_o = output.receiverPubKey                    // one-time output pubkey from DB
            val k_agg = signer.computeAggregatedSigningKey(k_o, K_o, kernelExcessScalar, kernelExcessCommitment)
            val K_agg = ECKey.fromPrivate(k_agg).pubKey        // inputPubKey (33 bytes)
            val outputId = computeOutputId(output)             // 32-byte BLAKE3 hash
            val sig = signer.signInput(k_agg, outputId)
            MwebInput(
                outputId = outputId,
                commitment = output.commitment,
                inputPubKey = K_agg,
                outputPubKey = K_o,
                signature = sig
            )
        }

        return PegOutResult(
            tx = MwebTx(inputs = inputs, kernels = listOf(kernel)),
            spentDbOutputIds = selected.map { it.outputId }   // commitment-hex, matches DB primary key
        )
    }

    /**
     * Estimates the fee for a peg-out transaction in satoshis.
     *
     * Per-input wire size:  32 + 33 + 33 + 33 + 64 = 195 bytes
     * Kernel wire size:     1 + varint(fee) + 1 + 8 + varint(scriptLen) + script + 33 + 64 ≈ 115 + scriptLen bytes
     */
    fun estimateFee(sendAmount: Long, feeRate: Int): Long {
        val selected = selectCoins(storage.getSpendableOutputs(), sendAmount)
        val scriptLen = 22   // typical P2WPKH scriptPubKey
        val txSize = selected.size * 195L + 115L + scriptLen
        return txSize * feeRate
    }

    // Greedy ascending coin selection: smallest UTXOs first until sum >= target
    private fun selectCoins(
        available: List<MwebDao.SpendableOutput>,
        target: Long
    ): List<MwebDao.SpendableOutput> {
        val result = mutableListOf<MwebDao.SpendableOutput>()
        var sum = 0L
        for (output in available) {   // already sorted ASC by DAO query
            if (sum >= target) break
            result.add(output)
            sum += output.value
        }
        return result
    }

    /**
     * Kernel signature message = BLAKE3(features || excess_commitment || fee_varint || pegout_count || amount_LE64 || scriptLen_varint || script)
     *
     * NOTE: Field order and encoding (varint vs LE64 for fee) should be confirmed against
     * src/libmw/include/mw/models/tx/Kernel.h → GetSignatureMessage().
     */
    private fun buildKernelMessage(
        features: Byte,
        fee: Long,
        pegOutAmount: Long,
        pegOutScript: ByteArray,
        excessCommitment: ByteArray
    ): ByteArray {
        val preimage = BitcoinOutput()
            .writeByte(features.toInt())
            .write(excessCommitment)                        // 33-byte Pedersen commitment
            .writeVarInt(fee)
            .writeVarInt(1L)                               // pegout count = 1
            .writeLong(pegOutAmount)                       // amount LE64
            .writeVarInt(pegOutScript.size.toLong())
            .write(pegOutScript)
            .toByteArray()
        return MwebHash.blake3(preimage)
    }

    /**
     * Computes output_id = BLAKE3(serialized_output).
     *
     * Serialization order: commitment(33) || senderPubKey(33) || receiverPubKey(33) ||
     *                      features(1) || maskedValue(8) || maskedNonce(4) || rangeProofBytes(var)
     *
     * NOTE: Field order should be verified against src/libmw/include/mw/models/tx/Output.h → GetOutputID().
     */
    private fun computeOutputId(output: MwebDao.SpendableOutput): ByteArray {
        val preimage = output.commitment +
            output.senderPubKey +
            output.receiverPubKey +
            byteArrayOf(output.features) +
            output.maskedValue +
            output.maskedNonce +
            output.rangeProofBytes
        return MwebHash.blake3(preimage)
    }
}
