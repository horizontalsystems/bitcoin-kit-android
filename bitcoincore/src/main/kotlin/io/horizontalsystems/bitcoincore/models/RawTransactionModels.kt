package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo

/** An output to include in a transaction built from raw scripts: locking script and value in satoshi. */
class RawOutput(val script: ByteArray, val value: Long)

/**
 * A transaction built by [io.horizontalsystems.bitcoincore.BitcoinCore.buildUnsignedTransaction]:
 * the serialized unsigned transaction, the wallet outputs it spends (in
 * input order), the fee it pays and where its change output is, if any.
 */
class UnsignedTransaction(
    val rawTransaction: ByteArray,
    val inputs: List<UnspentOutputInfo>,
    val fee: Long,
    val changeOutputIndex: Int?,
    val changeValue: Long?,
)
