package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.Fixtures
import io.horizontalsystems.bitcoinkit.crypto.CompactBits
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.math.BigInteger

object BlockHeaderValidatorTest : Spek({
    lateinit var validator: BlockHeaderValidator

    beforeEachTest {
        validator = BlockHeaderValidator()
    }

    describe("#validate") {
        it("passes when proof of work is valid") {
            val block = Fixtures.block2
            val previousBlock = Fixtures.block1
            assertDoesNotThrow {
                validator.validate(block, previousBlock)
            }
        }

        it("fails when proof of work is not valid") {
            val block = Fixtures.block2
            val previousBlock = Fixtures.block1
            block.bits = CompactBits.encode(BigInteger(block.headerHashReversedHex, 16).minus(BigInteger.valueOf(1L)))

            assertThrows<BlockValidatorException.InvalidProveOfWork> {
                validator.validate(block, previousBlock)
            }
        }
    }

})