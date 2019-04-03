package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class TestNet(storage: IStorage) : Network() {

    override var port: Int = 18333

    override var magic: Long = 0x0709110B
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"          // Bloq
    )

    private val blockHeader = BlockHeader(
            version = 2079170560,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000007524a71cc81cadbd1ddf9d38848fa8081ad2a72eade4b70d1c1"),
            merkleRoot = HashUtils.toBytesAsLE("975b76235d1a9b97fbf4a4f203a762728fb404d568dd33921e328e2d5a712c46"),
            timestamp = 1550688527,
            bits = 436465680,
            nonce = 489544448,
            hash = HashUtils.toBytesAsLE("00000000000002c23115a5766fc00c93711b30a8d2b8e6dde870c20da4d3e2fe")
    )

    override val checkpointBlock = Block(blockHeader, 1479744)
    override val blockValidator = TestnetValidator(this, storage)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
