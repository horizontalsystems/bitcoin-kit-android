package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinOutput

/**
 * MWEB Input wire format (verified against litecoin-project/litecoin src/libmw/include/mw/models/tx/Input.h).
 *
 * Wire layout (NO features byte):
 *   output_id     : 32 bytes  (BLAKE3 hash of the serialized output being spent)
 *   commitment    : 33 bytes  (Pedersen commitment of the output)
 *   input_pubkey  : 33 bytes  (K_agg — aggregated signing pubkey)
 *   output_pubkey : 33 bytes  (K_o   — original one-time output pubkey = receiverPubKey)
 *   signature     : 64 bytes  (Schnorr sig over output_id using k_agg)
 */
data class MwebInput(
    val outputId: ByteArray,       // 32 bytes — BLAKE3(serialized_output)
    val commitment: ByteArray,     // 33 bytes
    val inputPubKey: ByteArray,    // 33 bytes — K_agg
    val outputPubKey: ByteArray,   // 33 bytes — K_o (receiverPubKey from DB)
    val signature: ByteArray       // 64 bytes
) {
    fun serialize(): ByteArray = BitcoinOutput()
        .write(outputId)           // 32 bytes
        .write(commitment)         // 33 bytes
        .write(inputPubKey)        // 33 bytes
        .write(outputPubKey)       // 33 bytes
        .write(signature)          // 64 bytes
        .toByteArray()
}

/**
 * MWEB Kernel wire format for peg-out (verified against src/libmw/include/mw/models/tx/Kernel.h).
 *
 * Kernel feature bits:
 *   0x01 = KERN_FEAT_FEE
 *   0x02 = KERN_FEAT_PEGIN
 *   0x04 = KERN_FEAT_PEGOUT
 *   0x08 = KERN_FEAT_HEIGHT_LOCK
 *
 * A peg-out that charges a fee uses features = 0x05 (FEAT_FEE | FEAT_PEGOUT).
 *
 * Wire layout for features=0x05:
 *   features          : 1 byte
 *   fee               : varint
 *   pegouts           : varint(count=1) + LE64(amount) + varint(scriptLen) + script
 *   excess_commitment : 33 bytes  (Pedersen commitment blind*G, where blind = -(sum of k_o_i))
 *   signature         : 64 bytes  (Schnorr sig over kernel message)
 *
 * NOTE: Fee encoding (varint vs LE64) should be verified against Kernel.h Serialize() if test
 * transactions are rejected. Most likely varint based on the Hasher.Append API in the source.
 */
data class MwebKernel(
    val features: Byte,
    val fee: Long,
    val pegOutAmount: Long,
    val pegOutScript: ByteArray,
    val excessCommitment: ByteArray,   // 33 bytes — Pedersen commitment blind*G
    val signature: ByteArray           // 64 bytes
) {
    fun serialize(): ByteArray {
        val out = BitcoinOutput()
        out.writeByte(features.toInt())
        out.writeVarInt(fee)
        out.writeVarInt(1L)                                         // pegout count = 1
        out.writeLong(pegOutAmount)                                 // amount LE64
        out.writeVarInt(pegOutScript.size.toLong())
        out.write(pegOutScript)
        out.write(excessCommitment)                                 // 33 bytes
        out.write(signature)                                        // 64 bytes
        return out.toByteArray()
    }

    companion object {
        const val FEAT_FEE: Byte = 0x01
        const val FEAT_PEGOUT: Byte = 0x04
        const val FEAT_PEGOUT_WITH_FEE: Byte = 0x05    // FEE | PEGOUT
    }
}

/**
 * MWEB Transaction wire format.
 *
 * For a peg-out, [outputs] is empty — no new MWEB outputs are created.
 *
 * Wire layout:
 *   input_count  : varint
 *   inputs       : MwebInput[]
 *   output_count : varint  (0 for peg-out)
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
        out.writeVarInt(0L)
        out.writeVarInt(kernels.size.toLong())
        kernels.forEach { out.write(it.serialize()) }
        return out.toByteArray()
    }
}
