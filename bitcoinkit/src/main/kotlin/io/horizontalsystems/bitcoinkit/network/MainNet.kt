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
            "seed.bitcoin.jonasschnelli.ch",    // Jonas Schnelli
            "seed.btc.petertodd.org",           // Peter Todd
            "seed.bitcoin.sprovoost.nl"         // Sjors Provoost
    )

    private val blockHeader = Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("00000000000000000015fe695e8d2e5ed3a7de81d3818ef43a444e1ee7b3ace2")
        merkleHash = HashUtils.toBytesAsLE("aeee64cab37fb8f50fdbce4ff25dcb2223c099b01070a36cbaafc44d22da2a7f")
        timestamp = 1543838368
        bits = 389142908
        nonce = 512160369
    }

    override val checkpointBlock = Block(blockHeader, 552384)
    override val blockValidator = BlockValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
