package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.blocks.validators.BitcoinCashValidator
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class MainNetBitcoinCash : Network() {

    override var port: Int = 8333

    override var magic: Long = 0xe8f3e1e3L
    override var bip32HeaderPub: Int = 0x0488b21e
    override var bip32HeaderPriv: Int = 0x0488ade4
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "bitcoincash"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 0

    override val maxBlockSize = 32 * 1024 * 1024

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoinabc.org"
    )

    private val blockHeader = Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("0000000000000000012408c48907f199f8155330e4464dea078b34cd2633d1a4")
        merkleHash = HashUtils.toBytesAsLE("c3e1dcd7029186671cab622b3f444a2ab9bf738ca73d86e28a1202fbe0e2555e")
        timestamp = 1543990229
        bits = 403016521
        nonce = 1766777490
    }

    override val checkpointBlock = Block(blockHeader, 559478)
    override val blockValidator = BitcoinCashValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
