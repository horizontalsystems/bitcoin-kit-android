package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class RegTest : Network() {
    override var port: Int = 18444

    override var magic: Long = 0xdab5bffa
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000
    override val syncableFromApi = false

    override var dnsSeeds = listOf(
            "btc-regtest.horizontalsystems.xyz",
            "btc01-regtest.horizontalsystems.xyz",
            "btc02-regtest.horizontalsystems.xyz",
            "btc03-regtest.horizontalsystems.xyz"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 1,
            previousBlockHeaderHash = zeroHashBytes,
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1296688602,
            bits = 545259519,
            nonce = 2,
            hash = byteArrayOf()
    ), 0)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 1,
            previousBlockHeaderHash = zeroHashBytes,
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1296688602,
            bits = 545259519,
            nonce = 2,
            hash = byteArrayOf()
    ), 0)

}
