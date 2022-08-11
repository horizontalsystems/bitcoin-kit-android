package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    Logger.getLogger("").level = Level.SEVERE
    ////生成safe时屏掉
//    syncCheckpoints()
    buildCustomCheckpoint()
}

private fun syncCheckpoints() {
    BuildCheckpoints().sync()
    Thread.sleep(5000)
}

private fun buildCustomCheckpoint() {
    val checkpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("ba14fe39b34dcaba3f55c5e6b46d59dd5b3e4e6643e22f5c770c3bacbca68490"),
            merkleRoot = HashUtils.toBytesAsLE("60e2df26b450e33a48a9b677a1005a5a4e0781289337a7023ee59fda9ebc62bb"),
            timestamp = 1660228914,
            bits = 0,
            nonce = 91824240,
            hash = HashUtils.toBytesAsLE("454c196a9bf85ac79c4d574ac110db97a12ee421b1a22d0b734aade85d249581")
    ), 4112016)

    BuildCheckpoints().build(checkpointBlock)
}
