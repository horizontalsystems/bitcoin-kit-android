package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Embedded
import io.horizontalsystems.bitcoinkit.extensions.toReversedHex
import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.serializers.TransactionSerializer
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class BlockHeader(
        val version: Int,
        val previousBlockHeaderHash: ByteArray,
        val merkleRoot: ByteArray,
        val timestamp: Long,
        val bits: Long,
        val nonce: Long,
        val hash: ByteArray)

class FullTransaction(val header: Transaction, val inputs: List<TransactionInput>, val outputs: List<TransactionOutput>) {

    init {
        if (header.hashHexReversed.isEmpty()) {
            header.hash = HashUtils.doubleSha256(TransactionSerializer.serialize(this, withWitness = true))
            header.hashHexReversed = header.hash.toReversedHex()
        }

        inputs.forEach {
            it.transactionHashReversedHex = header.hashHexReversed
        }
        outputs.forEach {
            it.transactionHashReversedHex = header.hashHexReversed
        }
    }

}

class InputToSign(
        val input: TransactionInput,
        val previousOutput: TransactionOutput,
        val previousOutputPublicKey: PublicKey)

class InputWithBlock(
        @Embedded val input: TransactionInput,
        @Embedded val block: Block?)

class UnspentOutput(
        @Embedded val output: TransactionOutput,
        @Embedded val publicKey: PublicKey,
        @Embedded val transaction: Transaction,
        @Embedded val block: Block?)

class OutputWithPublicKey(
        @Embedded val output: TransactionOutput,
        @Embedded val publicKey: PublicKey)
