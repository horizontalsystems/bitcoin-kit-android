package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.util.logging.Level
import java.util.logging.Logger

// The :tools module wires up a JVM `main` SourceSet in tools/build.gradle so
// this can be run via Android Studio's green-arrow gutter. If you previously
// configured `-classpath $Classpath$:.../src/main/resources` in
// Edit Configurations... -> ToolsKt -> VM Options, clear that field — those
// VM args override the Gradle-provided classpath with a literal "$Classpath$".
fun main() {
    Logger.getLogger("").level = Level.SEVERE
    syncCheckpoints()
}

private fun syncCheckpoints() {
    BuildCheckpoints().sync()
    Thread.sleep(5000)
}

private fun buildCustomCheckpoint() {
    val checkpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000006bcf448b771c8f4db4e2ca653474e3b29504ec08422b3fba"),
            merkleRoot = HashUtils.toBytesAsLE("4ea18e999a57fc55fb390558dbb88a7b9c55c71c7de4cec160c045802ee587d2"),
            timestamp = 1397755646,
            bits = 419470732,
            nonce = 2160181286,
            hash = HashUtils.toBytesAsLE("00000000000000003decdbb5f3811eab3148fbc29d3610528eb3b50d9ee5723f")
    ), 296352)

    BuildCheckpoints().build(checkpointBlock)
}
