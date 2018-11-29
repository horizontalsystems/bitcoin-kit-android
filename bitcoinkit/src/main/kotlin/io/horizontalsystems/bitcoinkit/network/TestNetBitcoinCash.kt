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
        prevHash = HashUtils.toBytesAsLE("000000000000007de6117a4f0766212efebb95661660aa6d34adcc17e1d0621e")
        merkleHash = HashUtils.toBytesAsLE("987f271c0f7f1a7a3c9fff00f7603251ae8c4b4d184f800256eda6377c7d67a0")
        timestamp = 1543341600
        bits = 436289080
        nonce = 1941469020
    }, 1445760)

    override val blockValidator = TestnetBitcoinCashValidator(this)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validate(block, previousBlock)
    }
}
