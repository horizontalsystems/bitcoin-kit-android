package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetBitcoinCashValidator
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class TestNetBitcoinCash : Network() {

    override var port: Int = 18333

    override var magic: Long = 0xf4f3e5f4
    override var bip32HeaderPub: Int = 0x043587cf
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "bchtest"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 32 * 1024 * 1024

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoinabc.org",          // Bitcoin ABC seeder
            "testnet-seed-abc.bitcoinforks.org",    // bitcoinforks seeders
            "testnet-seed.bitprim.org",             // Bitprim
            "testnet-seed.deadalnix.me",            // Amaury SÉCHET
            "testnet-seeder.criptolayer.net"        // criptolayer.net
    )

    override val checkpointBlock = Block(Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("000000000dea8d3a526bc2d7b3a26588935992a1a412a6c5c449ffaa41b070b0")
        merkleHash = HashUtils.toBytesAsLE("dfa42c8fc3d8bac6d6fb51007128092f41d590ace1b3522af7062b8a848ebde7")
        timestamp = 1551085591
        bits = 486604799
        nonce = 1684221831
    }, 1287761)

    override val blockValidator = TestnetBitcoinCashValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
