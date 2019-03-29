package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class RegTest(storage: IStorage) : Network() {
    override var port: Int = 18444

    override var magic: Long = 0xdab5bffa
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "btc-regtest.horizontalsystems.xyz",
            "btc01-regtest.horizontalsystems.xyz",
            "btc02-regtest.horizontalsystems.xyz",
            "btc03-regtest.horizontalsystems.xyz"
    )

    override val blockValidator = TestnetValidator(this, storage)

    private val blockHeader = BlockHeader(
            version = 1,
            previousBlockHeaderHash = zeroHashBytes,
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1296688602,
            bits = 545259519,
            nonce = 2
    )

    override val checkpointBlock = Block(blockHeader, 0)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validateHeader(block, previousBlock)
    }
}
