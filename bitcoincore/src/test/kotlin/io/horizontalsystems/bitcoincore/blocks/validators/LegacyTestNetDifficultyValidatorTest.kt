package io.horizontalsystems.bitcoincore.blocks.validators

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object LegacyTestNetDifficultyValidatorTest : Spek({
    val storage = mock<IStorage>()

    val validator by memoized {
        LegacyTestNetDifficultyValidator(storage, 2016, 14 * 24 * 60 * 60, 0x1d00ffff)
    }

    describe("#isBlockValidatable") {
        val block = mock<Block>()
        val previousBlock = mock<Block>()

        it("is true when prev block is later then February 16th 2012") {
            whenever(previousBlock.timestamp).thenReturn(1329264000 + 1)
            Assert.assertTrue(validator.isBlockValidatable(block, previousBlock))
        }

        it("is false when prev block is earlier or equal to February 16th 2012") {
            whenever(previousBlock.timestamp).thenReturn(1329264000)
            Assert.assertFalse(validator.isBlockValidatable(block, previousBlock))
        }
    }

    describe("#validate") {
        it("passes") {
            val check1 = Block(
                    height = 1411200,
                    header = BlockHeader(
                            version = 536870912,
                            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000a5bf9029aebb1956200304ffee31bc09f1323ae412d81fa2b2"),
                            merkleRoot = HashUtils.toBytesAsLE("dff076f1f3468f86785b42c10e6f23c849ccbc1d40a0fa8909b20b20fb204de2"),
                            timestamp = 1535560970,
                            bits = 424329477,
                            nonce = 2681700833,
                            hash = byteArrayOf(1)
                    )
            )

            var prevBlock = check1
            val blockHead = BlockHeader(
                    version = 536870912,
                    previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000000003e3b50c7edca7bf59075b3d39ee2668076aa1ebe559787ff25"),
                    merkleRoot = HashUtils.toBytesAsLE("6a05a10911d844e86e7758bf27ce183b2eaa5768108d992efdb6487c8f3f6dae"),
                    timestamp = 1536796796,
                    bits = 424329477,
                    nonce = 915088888,
                    hash = byteArrayOf(1)
            )

            for (i in 1 until 2016) {
                prevBlock = Block(blockHead, prevBlock)
            }

            val check2Head = BlockHeader(
                    version = 536870912,
                    previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000046f38ada53de3346d8191f69c8f3c0ba9e1950f5bf291989c4"),
                    merkleRoot = HashUtils.toBytesAsLE("827bc2d47a164b9144a507eebc40b32f8f3e7e8c784b17e0a1fa245bfe9c100c"),
                    timestamp = 1536797113,
                    nonce = 1267362056,
                    bits = 424435696,
                    hash = byteArrayOf(1)
            )

            val check2 = Block(check2Head, prevBlock)

            whenever(storage.getBlock(hashHash = any())).thenReturn(check1)
            whenever(storage.getBlock(hashHash = check2.previousBlockHash)).thenReturn(prevBlock)

            Assertions.assertDoesNotThrow {
                validator.validate(check2, prevBlock)
            }
        }
    }
})
