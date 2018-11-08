package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetValidator
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class TestNet : NetworkParameters() {

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

    private val blockHeader = Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("000000000000032d74ad8eb0a0be6b39b8e095bd9ca8537da93aae15087aafaf")
        merkleHash = HashUtils.toBytesAsLE("dec6a6b395b29be37f4b074ed443c3625fac3ae835b1f1080155f01843a64268")
        timestamp = 1533498326
        bits = 436270990
        nonce = 205753354
    }

    override val checkpointBlock = Block(blockHeader, 1380960)
    override val blockValidator = TestnetValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
