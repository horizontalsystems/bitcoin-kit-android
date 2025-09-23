package io.horizontalsystems.tools

import io.horizontalsystems.tools.chains.MainNet
import io.horizontalsystems.tools.chains.MainNetBitcoinCash
import io.horizontalsystems.tools.chains.MainNetDash
import io.horizontalsystems.tools.chains.MainNetECash
import io.horizontalsystems.tools.chains.MainNetLitecoin
import io.horizontalsystems.tools.chains.TestNet
import io.horizontalsystems.tools.chains.TestNetBitcoinCash
import io.horizontalsystems.tools.chains.TestNetDash
import io.horizontalsystems.tools.chains.TestNetLitecoin
import io.horizontalsystems.tools.extensions.toHexString
import io.horizontalsystems.tools.io.BitcoinOutput
import io.horizontalsystems.tools.network.Network
import io.horizontalsystems.tools.pack.Block
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

class BuildCheckpoints : CheckpointSyncer.Listener {

    private val syncers = mutableListOf<CheckpointSyncer>().also {
        // Bitcoin
        it.add(CheckpointSyncer(MainNet(), 2016, 1, this))
//        it.add(CheckpointSyncer(TestNet(), 2016, 1, this))

        // Bitcoin Cash
        it.add(CheckpointSyncer(MainNetBitcoinCash(), 147, 147, this))
//        it.add(CheckpointSyncer(TestNetBitcoinCash(), 147, 147, this))

        // Dash
        it.add(CheckpointSyncer(MainNetDash(), 24, 24, this))
//        it.add(CheckpointSyncer(TestNetDash(), 24, 24, this))

        // Litecoin
        it.add(CheckpointSyncer(MainNetLitecoin(), 2016, 2, this))
//        it.add(CheckpointSyncer(TestNetLitecoin(), 2016, 2, this))

        // Ecash
        it.add(CheckpointSyncer(MainNetECash(), 147, 147, this))
    }

    fun build(checkpoint: Block) {
        println(" ================== CHECKPOINT ================== ")
        println(serialize(checkpoint).toHexString())
        println(" ================================================ ")
    }

    fun sync() {
        syncers.forEach { it.start() }
    }

    override fun onSync(network: Network, checkpoints: List<Block>) {
        val networkName = network.javaClass.simpleName
        val checkpointFile = "${packagePath(network)}/src/main/resources/${networkName}.checkpoint"

        writeCheckpoints(checkpointFile, checkpoints)

        println("Synced: ${syncers.count { it.isSynced }}")
        println("Remaining: ${syncers.count { !it.isSynced }}")

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

        checkpoints.forEach {
            buffer.put(serialize(it))
            writer.println(buffer.array().toHexString())
            buffer.clear()
        }

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
            is MainNetLitecoin,
            is TestNetLitecoin -> "litecoinkit"
            is MainNetECash -> "ecashkit"
            else -> throw Exception("Invalid network: ${network.javaClass.name}")
        }
    }
}
