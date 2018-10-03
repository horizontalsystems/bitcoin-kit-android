package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.BlockValidator
import bitcoin.wallet.kit.blocks.BlockValidatorException
import bitcoin.wallet.kit.models.Block
import bitcoin.walllet.kit.utils.HashUtils
import bitcoin.walllet.kit.utils.Utils

/** Network-specific parameters */
abstract class NetworkParameters {

    // The strings returned by getId() for networks
    val ID_MAINNET = "org.bitcoin.production"
    val ID_TESTNET = "org.bitcoin.test"
    val ID_REGTEST = "org.bitcoin.regtest"

    val protocolVersion = 70014
    val bloomFilter = 70000
    val networkServices = 1L
    val serviceFullNode = 1L
    val zeroHashBytes = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000")

    val maxTargetBits = Utils.decodeCompactBits(0x1d00ffffL)    // Maximum difficulty
    val targetTimespan: Long = 14 * 24 * 60 * 60                        // 2 weeks per difficulty cycle, on average.
    val targetSpacing = 10 * 60                                         // 10 minutes per block.
    var heightInterval = targetTimespan / targetSpacing                 // 2016 blocks

    abstract var id: String
    abstract var port: Int

    // Indicates message origin network and is used to seek to the next message when stream state is unknown.
    abstract var packetMagic: Long
    abstract var bip32HeaderPub: Int
    abstract var bip32HeaderPriv: Int
    abstract var addressHeader: Int
    abstract var scriptAddressHeader: Int
    abstract var coinType: Int
    abstract var dnsSeeds: Array<String>
    abstract var addressSegwitHrp: String

    abstract val checkpointBlock: Block
    abstract fun validate(block: Block, previousBlock: Block)

    fun magicAsUInt32ByteArray(): ByteArray {
        val magic = packetMagic
        val bytes = ByteArray(4)
        bytes[3] = (magic and 0xFFFF).toByte()
        bytes[2] = ((magic ushr 8) and 0xFFFF).toByte()
        bytes[1] = ((magic ushr 16) and 0xFFFF).toByte()
        bytes[0] = ((magic ushr 24) and 0xFFFF).toByte()
        return bytes
    }

    fun isDifficultyTransitionEdge(height: Int): Boolean {
        return (height % heightInterval == 0L)
    }

    open fun checkDifficultyTransitions(block: Block) {
        val lastCheckPointBlock = BlockValidator.getLastCheckPointBlock(block)

        val previousBlock = checkNotNull(block.previousBlock) {
            throw BlockValidatorException.NoPreviousBlock()
        }

        val blockHeader = previousBlock.header
        val lastCheckPointBlockHeader = lastCheckPointBlock.header

        if (blockHeader == null || lastCheckPointBlockHeader == null) {
            throw BlockValidatorException.NoHeader()
        }

        // Limit the adjustment step
        var timespan = blockHeader.timestamp - lastCheckPointBlockHeader.timestamp
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4

        var newTarget = Utils.decodeCompactBits(blockHeader.bits)
        newTarget = newTarget.multiply(timespan.toBigInteger())
        newTarget = newTarget.divide(targetTimespan.toBigInteger())

        // Difficulty hit proof of work limit: newTarget.toString(16)
        if (newTarget > maxTargetBits) {
            newTarget = maxTargetBits
        }

        val newTargetCompact = Utils.encodeCompactBits(newTarget)
        if (newTargetCompact != block.header?.bits) {
            throw BlockValidatorException.NotDifficultyTransitionEqualBits()
        }
    }
}
