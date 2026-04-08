package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinOutput

/**
 * MWEB Input wire format (LIP-0003).
 *
 * Wire layout:
 *   features      : 1 byte   (0x00 = standard, 0x01 = pegin)
 *   output_id     : 33 bytes (commitment of the UTXO being spent)
 *   commitment    : 33 bytes (same as output_id for standard inputs)
 *   input_pubkey  : 33 bytes (one-time spend pubkey Ko)
 *   signature     : 64 bytes (Schnorr sig over the input message)
 *
 * CAVEAT: The exact input message signed (preimage fed into SHA-256 before Schnorr signing)
 * must be verified against litecoin-project/litecoin src/mweb/mweb_transact.cpp.
 */
data class MwebInput(
    val features: Byte,
    val outputId: ByteArray,      // 33 bytes — commitment of the UTXO
    val commitment: ByteArray,    // 33 bytes — same as outputId for standard inputs
    val inputPubKey: ByteArray,   // 33 bytes — Ko (one-time spend pubkey)
    val signature: ByteArray      // 64 bytes — Schnorr sig
) {
    fun serialize(): ByteArray = BitcoinOutput()
        .writeByte(features.toInt())
        .write(outputId)
        .write(commitment)
        .write(inputPubKey)
        .write(signature)
        .toByteArray()
}

/**
 * MWEB Kernel wire format for peg-out (LIP-0003).
 *
 * Kernel feature bits:
 *   0x01 = KERN_FEAT_PEGGED_IN
 *   0x02 = KERN_FEAT_PLAIN (no extra fields)
 *   0x04 = KERN_FEAT_HEIGHT_LOCKED
 *   0x08 = KERN_FEAT_PEGGED_OUT
 *
 * Wire layout for peg-out (features = 0x08):
 *   features        : 1 byte
 *   fee             : 8 bytes LE uint64
 *   peg_out_amount  : 8 bytes LE uint64
 *   script_len      : varint
 *   peg_out_script  : script_len bytes (scriptPubKey of destination canonical address)
 *   excess_pubkey   : 33 bytes (sum of input spend pubkeys)
 *   signature       : 64 bytes (Schnorr sig over the kernel message)
 *
 * CAVEAT: The exact kernel message hash construction must be verified against
 * litecoin-project/litecoin src/mweb/mweb_transact.cpp::Kernel::GetMessageHash().
 */
data class MwebKernel(
    val features: Byte,
    val fee: Long,
    val pegOutAmount: Long,
    val pegOutScript: ByteArray,
    val excessPubKey: ByteArray,  // 33 bytes
    val signature: ByteArray      // 64 bytes
) {
    fun serialize(): ByteArray {
        val out = BitcoinOutput()
        out.writeByte(features.toInt())
        out.writeLong(fee)
        out.writeLong(pegOutAmount)
        out.writeVarInt(pegOutScript.size.toLong())
        out.write(pegOutScript)
        out.write(excessPubKey)
        out.write(signature)
        return out.toByteArray()
    }

    companion object {
        const val FEAT_PEGGED_OUT: Byte = 0x08
    }
}

/**
 * MWEB Transaction wire format.
 *
 * For a peg-out, [outputs] is empty (no new MWEB outputs are created).
 * Generating MWEB outputs requires Bulletproof range proofs (not yet supported).
 *
 * Wire layout:
 *   input_count  : varint
 *   inputs       : MwebInput[]
 *   output_count : varint (0 for peg-out)
 *   kernel_count : varint
 *   kernels      : MwebKernel[]
 */
data class MwebTx(
    val inputs: List<MwebInput>,
    val kernels: List<MwebKernel>
) {
    fun serialize(): ByteArray {
        val out = BitcoinOutput()
        out.writeVarInt(inputs.size.toLong())
        inputs.forEach { out.write(it.serialize()) }
        out.writeVarInt(0L)  // no MWEB outputs for peg-out
        out.writeVarInt(kernels.size.toLong())
        kernels.forEach { out.write(it.serialize()) }
        return out.toByteArray()
    }
}
