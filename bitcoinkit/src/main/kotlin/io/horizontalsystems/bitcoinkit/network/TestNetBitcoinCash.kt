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
            "testnet-seed.bitcoinabc.org"
    )

    override val checkpointBlock = Block(Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("000000000000043c25d4b23dee40208a9df99ef5717d236379120b01af2077e2")
        merkleHash = HashUtils.toBytesAsLE("fb7ca6fbd9e1dd307cdafa7f7bf66317a49bfae4fc8e4d841f4faaf1acae5844")
        timestamp = 1543989687
        bits = 486604799
        nonce = 890299933
    }, 1272398)

    override val blockValidator = TestnetBitcoinCashValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
