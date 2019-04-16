package io.horizontalsystems.bitcoinkit.bitcoincash.blocks

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.Fixtures
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.security.SecureRandom

object BitcoinCashBlockValidatorHelperTest : Spek({
    lateinit var helper: BitcoinCashBlockValidatorHelper

    beforeEachTest {
        helper = BitcoinCashBlockValidatorHelper(storage)
    }

    describe("#medianTimePast") {
        it("passes") {
            val block3 = Fixtures.block3
            block3.timestamp = 1000

            val block = chain(block3, 11)

            val medianTime = helper.medianTimePast(block)
            Assert.assertEquals(1600, medianTime)

        }
    }

    describe("#getSuitableBlock") {
        it("getSuitableBlock") {
            val block1 = Block(height = 1, header = header(10))
            val block2 = Block(height = 2, header = header(20))
            val block3 = Block(height = 3, header = header(30))

            whenever(storage.getBlock(hashHex = block2.previousBlockHashReversedHex)).thenReturn(block1)
            whenever(storage.getBlock(hashHex = block3.previousBlockHashReversedHex)).thenReturn(block2)

            Assert.assertEquals(block2, helper.getSuitableBlock(mutableListOf(block1, block2, block3)))
        }

        it("getSuitableBlock_sameTime") {
            val block1 = Block(height = 1, header = header(1536779466))
            val block2 = Block(height = 2, header = header(1536780486))
            val block3 = Block(height = 3, header = header(1536780486))

            whenever(storage.getBlock(hashHex = block2.previousBlockHashReversedHex)).thenReturn(block1)
            whenever(storage.getBlock(hashHex = block3.previousBlockHashReversedHex)).thenReturn(block2)

            Assert.assertEquals(block2, helper.getSuitableBlock(mutableListOf(block1, block2, block3)))
        }
    }


})

private val storage = mock<IStorage>()
private val secRandom = SecureRandom()

private fun chain(block: Block, size: Int, timeInterval: Int = 100): Block {
    var currentBlock = block

    lateinit var nextBlock: Block

    for (i in 0 until size) {
        val hash = ByteArray(5)
        secRandom.nextBytes(hash)

        val header = BlockHeader(
                version = 0,
                timestamp = currentBlock.timestamp + timeInterval,
                previousBlockHeaderHash = hash,
                merkleRoot = byteArrayOf(),
                bits = 0,
                nonce = 0,
                hash = byteArrayOf()
        )

        nextBlock = Block(header, currentBlock)

        whenever(storage.getBlock(hashHex = nextBlock.previousBlockHashReversedHex)).thenReturn(currentBlock)

        currentBlock = nextBlock
    }

    return currentBlock
}

private fun header(time: Long): BlockHeader {
    val hash = ByteArray(5)
    secRandom.nextBytes(hash)

    return BlockHeader(
            version = 0,
            timestamp = time,
            previousBlockHeaderHash = hash,
            merkleRoot = byteArrayOf(),
            bits = 0,
            nonce = 0,
            hash = byteArrayOf(1)
    )
}

