package io.horizontalsystems.litecoinkit.mweb.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Raw MWEB output as received from the P2P network.
 *
 * Fields map directly to the MWEB Output wire format (LIP-0003):
 *   commitment    – 33-byte Pedersen commitment (v·H + r·G)
 *   senderPubKey  – 33-byte ephemeral pubkey Ke (from sender)
 *   receiverPubKey– 33-byte one-time pubkey Ko (derived by sender for recipient)
 *   maskedValue   – 8-byte XOR-masked amount
 *   maskedNonce   – 4-byte XOR-masked nonce (for key derivation)
 */
@Entity(tableName = "mweb_outputs")
data class MwebOutput(
    @PrimaryKey val outputId: String,       // lowercase hex of commitment
    val commitment: ByteArray,              // 33 bytes
    val senderPubKey: ByteArray,            // 33 bytes (Ke)
    val receiverPubKey: ByteArray,          // 33 bytes (Ko)
    val features: Byte,                     // OutputMessage feature flags
    val maskedValue: ByteArray,             // 8 bytes
    val maskedNonce: ByteArray,             // 4 bytes
    val rangeProofSize: Int,                // for informational purposes
    val leafIndex: Long,                    // global MWEB UTXO set index
    val blockHash: String                   // canonical block hash (hex)
)