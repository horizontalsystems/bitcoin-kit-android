package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidator
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class MainNet : Network() {

    override var port: Int = 8333

    override var magic: Long = 0xd9b4bef9L
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 0

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoin.sipa.be",             // Pieter Wuille
            "dnsseed.bluematt.me",              // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",       // Luke Dashjr
            "seed.bitcoinstats.com",            // Chris Decker
            "seed.bitnodes.io",                 // Addy Yeow
            "seed.bitcoin.jonasschnelli.ch",    // Jonas Schnelli
            "seed.btc.petertodd.org",           // Peter Todd
            "seed.bitcoin.sprovoost.nl"         // Sjors Provoost
    )

    private val blockHeader = Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5")
        merkleHash = HashUtils.toBytesAsLE("167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541")
        timestamp = 1533980459
        bits = 388763047
        nonce = 1545867530
    }

    override val checkpointBlock = Block(blockHeader, 536256)
    override val blockValidator = BlockValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
