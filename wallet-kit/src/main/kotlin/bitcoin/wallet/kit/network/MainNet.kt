package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.BlockValidator
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

class MainNet : NetworkParameters() {

    override var port: Int = 8333

    override var magic: Long = 0xd9b4bef9L
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 0

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoin.sipa.be",             // Pieter Wuille
            "dnsseed.bluematt.me",              // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",       // Luke Dashjr
            "seed.bitcoinstats.com",            // Chris Decker
            "seed.bitnodes.io",                 // Addy Yeow
            "bitseed.xf2.org",                  // Jeff Garzik
            "seed.bitcoin.jonasschnelli.ch",    // Jonas Schnelli
            "bitcoin.bloqseeds.net"             // Bloq
    )

    override val checkpointBlock = Block(
            Header().apply {
                version = 536870912
                prevHash = HashUtils.toBytesAsLE("00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5")
                merkleHash = HashUtils.toBytesAsLE("167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541")
                timestamp = 1533980459
                bits = 388763047
                nonce = 1545867530
            },
            536256)

    override fun validate(block: Block, previousBlock: Block) {
        BlockValidator.validateHeader(block, previousBlock)

        if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else {
            BlockValidator.validateBits(block, previousBlock)
        }
    }
}
