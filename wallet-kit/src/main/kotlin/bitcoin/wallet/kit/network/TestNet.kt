package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.BlockValidator
import bitcoin.wallet.kit.blocks.BlockValidatorException
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

open class TestNet : NetworkParameters() {
    private val diffDate = 1329264000L // February 16th 2012

    override var id: String = ID_TESTNET
    override var port: Int = 18333

    override var magic: Long = 0x0709110B
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"          // Bloq
    )

    override val checkpointBlock = Block(
            Header().apply {
                version = 536870912
                prevHash = HashUtils.toBytesAsLE("000000000000032d74ad8eb0a0be6b39b8e095bd9ca8537da93aae15087aafaf")
                merkleHash = HashUtils.toBytesAsLE("dec6a6b395b29be37f4b074ed443c3625fac3ae835b1f1080155f01843a64268")
                timestamp = 1533498326
                bits = 436270990
                nonce = 205753354
            },
            1380960)

    override fun validate(block: Block, previousBlock: Block) {
        BlockValidator.validateHeader(block, previousBlock)

        if (isDifficultyTransitionEdge(block.height)) {
            checkDifficultyTransitions(block)
        } else {
            BlockValidator.validateBits(block, previousBlock)
        }
    }

    override fun checkDifficultyTransitions(block: Block) {
        var previousBlock = checkNotNull(block.previousBlock) { throw BlockValidatorException.NoPreviousBlock() }
        val previousBlockHeader = checkNotNull(previousBlock.header) {
            throw BlockValidatorException.NoHeader()
        }

        if (previousBlockHeader.timestamp > diffDate) {
            val blockHeader = checkNotNull(block.header) {
                throw BlockValidatorException.NoHeader()
            }

            val timeDelta = blockHeader.timestamp - previousBlockHeader.timestamp
            if (timeDelta >= 0 && timeDelta <= targetSpacing * 2) {
                var cursor = block
                var cursorHeader = checkNotNull(cursor.header)


                while (cursor.height != 0 && (cursor.height % heightInterval.toInt()) != 0 && cursorHeader.bits == maxTargetBits.toLong()) {
                    previousBlock = checkNotNull(cursor.previousBlock) {
                        throw BlockValidatorException.NoPreviousBlock()
                    }

                    val header = checkNotNull(previousBlock.header) {
                        throw BlockValidatorException.NoHeader()
                    }

                    cursor = previousBlock
                    cursorHeader = header
                }

                if (cursorHeader.bits != blockHeader.bits) {
                    BlockValidatorException.NotEqualBits()
                }
            }
        } else super.checkDifficultyTransitions(block)
    }
}
