package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Embedded
import io.horizontalsystems.bitcoinkit.models.*

class BlockHeader(
        val version: Int,
        val previousBlockHeaderHash: ByteArray,
        val merkleRoot: ByteArray,
        val timestamp: Long,
        val bits: Long,
        val nonce: Long,
        val hash: ByteArray)

class FullTransaction(
        val header: Transaction,
        val inputs: List<TransactionInput>,
        val outputs: List<TransactionOutput>)

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
