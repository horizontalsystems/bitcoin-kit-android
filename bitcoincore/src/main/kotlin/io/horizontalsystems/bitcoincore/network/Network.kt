package io.horizontalsystems.bitcoincore.network

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.bitcoincore.utils.HashUtils

/** Network-specific parameters */
abstract class Network {

    open val protocolVersion = 70014
    open val syncableFromApi = true
    val bloomFilter = 70000
    val networkServices = 0L
    val serviceFullNode = 1L
    val zeroHashBytes = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000")

    abstract val maxBlockSize: Int

    abstract var port: Int
    abstract var magic: Long
    abstract var bip32HeaderPub: Int
    abstract var bip32HeaderPriv: Int
    abstract var coinType: Int
    abstract var dnsSeeds: Array<String>
    abstract var addressVersion: Int
    abstract var addressSegwitHrp: String
    abstract var addressScriptVersion: Int

    abstract val checkpointBlock: Block
    open val sigHashForked: Boolean = false
    open val sigHashValue = Sighash.ALL
}
