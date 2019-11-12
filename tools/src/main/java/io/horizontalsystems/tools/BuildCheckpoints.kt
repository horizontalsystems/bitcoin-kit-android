package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoincash.TestNetBitcoinCash
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.bitcoinkit.TestNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.dashkit.TestNetDash
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

fun main() {
    Logger.getLogger("").level = Level.SEVERE
    BuildCheckpoints()
    Thread.sleep(5000)
}

class BuildCheckpoints : CheckpointSyncer.Listener {

    private val syncers = mutableListOf<CheckpointSyncer>().also {
        // Bitcoin
        it.add(CheckpointSyncer(MainNet(), 2016, this))
        it.add(CheckpointSyncer(TestNet(), 2016, this))

        // Bitcoin Cash
        it.add(CheckpointSyncer(MainNetBitcoinCash(), 147, this))
        it.add(CheckpointSyncer(TestNetBitcoinCash(), 147, this))

        // Dash
        it.add(CheckpointSyncer(MainNetDash(), 24, this))
        it.add(CheckpointSyncer(TestNetDash(), 24, this))
    }

    init {
        syncers.forEach { it.start() }
    }

    override fun onSync(network: Network, checkpoints: List<Block>) {
        val networkName = network.javaClass.simpleName
        val checkpointFile = "${packagePath(network)}/src/main/resources/${networkName}.checkpoint"

        writeCheckpoints(checkpointFile, checkpoints)

        if (syncers.none { !it.isSynced }) {
            exitProcess(0)
        }
    }

    // Writing to file

    private fun writeCheckpoints(checkpointFile: String, checkpoints: List<Block>) {
        val file = File(checkpointFile)
        val fileOutputStream: OutputStream = FileOutputStream(file)
        val outputStream: Writer = OutputStreamWriter(fileOutputStream, StandardCharsets.US_ASCII)

        val buffer = ByteBuffer.allocate(80 + 4 + 32) // header + block height + block hash
        val writer = PrintWriter(outputStream)

        val checkpoint = checkpoints.last()
        buffer.put(serialize(checkpoint))
        writer.println(buffer.array().toHexString())
        writer.close()
    }

    private fun serialize(block: Block): ByteArray {
        val payload = BitcoinOutput().also {
            it.writeInt(block.version)
            it.write(block.previousBlockHash)
            it.write(block.merkleRoot)
            it.writeUnsignedInt(block.timestamp)
            it.writeUnsignedInt(block.bits)
            it.writeUnsignedInt(block.nonce)
            it.writeInt(block.height)
            it.write(block.headerHash)
        }

        return payload.toByteArray()
    }

    private fun packagePath(network: Network): String {
        return when (network) {
            is MainNet,
            is TestNet -> "bitcoinkit"
            is MainNetBitcoinCash,
            is TestNetBitcoinCash -> "bitcoincashkit"
            is MainNetDash,
            is TestNetDash -> "dashkit"
            else -> throw Exception("Invalid network")
        }
    }
}
