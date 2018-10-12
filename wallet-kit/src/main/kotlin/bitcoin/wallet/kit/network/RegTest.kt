package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.TestnetValidator
import bitcoin.wallet.kit.models.Block

class RegTest : NetworkParameters() {
    override var port: Int = 18444

    override var magic: Long = 0x0709110B
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override var dnsSeeds: Array<String> = arrayOf(
            "blocknode01.grouvi.org",
            "blocknode02.grouvi.org",
            "blocknode03.grouvi.org",
            "blocknode04.grouvi.org"
    )

    override val blockValidator = TestnetValidator(this)
    override val checkpointBlock = Block()

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validateHeader(block, previousBlock)
    }
}
