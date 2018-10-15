package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.TestnetValidator
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

class RegTest : NetworkParameters() {
    override var port: Int = 18444

    override var magic: Long = 0xfabfb5da
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

    private val blockHeader = Header().apply {
        version = 536870912
        prevHash = HashUtils.toBytesAsLE("00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5")
        merkleHash = HashUtils.toBytesAsLE("167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541")
        timestamp = 1533980459
        bits = 388763047
        nonce = 1545867530
    }

    override val checkpointBlock = Block(blockHeader, 0)

    override fun validateBlock(block: Block, previousBlock: Block) {
        blockValidator.validateHeader(block, previousBlock)
    }
}
