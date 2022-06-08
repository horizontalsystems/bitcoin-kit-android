package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

fun main() {
//    Logger.getLogger("").level = Level.SEVERE
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
            previousBlockHeaderHash = HashUtils.toBytesAsLE("b6d9f1e0e0abfef565e4130572f0ff66f22817689bb20be1c4afd632f36ab21f"),
            merkleRoot = HashUtils.toBytesAsLE("c466dc598b7f66094557ba1c4939315f258425724d869bcde5356813bf657ef0"),
            timestamp = 1654694154,
            bits = 0,
            nonce = 86431590,
            hash = HashUtils.toBytesAsLE("546e29c7d3845e897fafca60e2f00e9238e9437a6e1e8e38510921b189f7db12")
    ), 3932261)

    BuildCheckpoints().build(checkpointBlock)
}
