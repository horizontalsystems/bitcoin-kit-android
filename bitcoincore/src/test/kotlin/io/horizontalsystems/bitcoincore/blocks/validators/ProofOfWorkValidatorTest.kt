package io.horizontalsystems.bitcoincore.blocks.validators

import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.math.BigInteger

object ProofOfWorkValidatorTest : Spek({
    lateinit var validator: ProofOfWorkValidator

    beforeEachTest {
        validator = ProofOfWorkValidator()
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
            block.bits = CompactBits.encode(BigInteger(block.headerHash).minus(BigInteger.valueOf(1L)))

            assertThrows<BlockValidatorException.InvalidProofOfWork> {
                validator.validate(block, previousBlock)
            }
        }
    }

})
