package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    Logger.getLogger("").level = Level.SEVERE
    ////生成safe时屏掉
    syncCheckpoints()
    buildCustomCheckpoint()
}

private fun syncCheckpoints() {
    BuildCheckpoints().sync()
    Thread.sleep(5000)
}

private fun buildCustomCheckpoint() {
    val checkpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("28d14d6a1119f4ded298ed253251ece7ca87eaa493d3b27af74e883359b4c09b"),
            merkleRoot = HashUtils.toBytesAsLE("b68af25ff5638a6821d80a8aa2c9b18347d4a54f27b9098351dd6e95e6c8db26"),
            timestamp = 1655733594,
            bits = 0,
            nonce = 87468750,
            hash = HashUtils.toBytesAsLE("b68af25ff5638a6821d80a8aa2c9b18347d4a54f27b9098351dd6e95e6c8db26")
    ), 3966833)

    BuildCheckpoints().build(checkpointBlock)
}
