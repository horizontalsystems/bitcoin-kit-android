package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.utils.HashUtils

/** Network-specific parameters */
abstract class Network {

    open val protocolVersion = 70014
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

    open fun generateBlockHeaderHash(data: ByteArray): ByteArray {
        return HashUtils.doubleSha256(data)
    }
}
