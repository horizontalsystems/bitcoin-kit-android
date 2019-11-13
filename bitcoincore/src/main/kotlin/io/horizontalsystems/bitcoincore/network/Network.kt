package io.horizontalsystems.bitcoincore.network

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader

abstract class Network {

    open val protocolVersion = 70014
    open val syncableFromApi = true
    val bloomFilter = 70000
    val networkServices = 0L
    val serviceFullNode = 1L
    val zeroHashBytes = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000")

    abstract val maxBlockSize: Int
    abstract val dustRelayTxFee: Int

    abstract var port: Int
    abstract var magic: Long
    abstract var bip32HeaderPub: Int
    abstract var bip32HeaderPriv: Int
    abstract var coinType: Int
    abstract var dnsSeeds: List<String>
    abstract var addressVersion: Int
    abstract var addressSegwitHrp: String
    abstract var addressScriptVersion: Int

    abstract val bip44CheckpointBlock: Block
    open val lastCheckpointBlock: Block = readLastCheckpoint()

    open val sigHashForked: Boolean = false
    open val sigHashValue = Sighash.ALL

    private fun readLastCheckpoint(): Block {
        val stream = javaClass.classLoader?.getResourceAsStream("${javaClass.simpleName}.checkpoint")
        val inputStreamReader: Reader = InputStreamReader(stream)
        val reader = BufferedReader(inputStreamReader)
        val checkpoint = reader.readLine()

        return BitcoinInput(checkpoint.hexToByteArray()).use { input ->
            val version = input.readInt()
            val prevHash = input.readBytes(32)
            val merkleHash = input.readBytes(32)
            val timestamp = input.readUnsignedInt()
            val bits = input.readUnsignedInt()
            val nonce = input.readUnsignedInt()
            val height = input.readInt()
            val hash = input.readBytes(32)

            Block(BlockHeader(
                    version = version,
                    previousBlockHeaderHash = prevHash,
                    merkleRoot = merkleHash,
                    timestamp = timestamp,
                    bits = bits,
                    nonce = nonce,
                    hash = hash
            ), height)
        }
    }
}
