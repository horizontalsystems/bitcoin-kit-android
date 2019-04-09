package io.horizontalsystems.bitcoinkit.bitcoincash.blocks.validators

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.managers.BlockHelper
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network
import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DAAValidatorTest : Spek({
    lateinit var validator: DAAValidator
    val network = mock<Network>()
    val storage = mock<IStorage>()
    val blockHelper = mock<BlockHelper>()
    val block = mock<Block>()
    val previousBlock = mock<Block>()

    beforeEachTest {
        validator = DAAValidator(network, storage, blockHelper)
    }

    describe("#isBlockValidatable") {
        it("is true when median time past later then or equal to 2017 November 3, 14:06 GMT") {
            whenever(blockHelper.medianTimePast(block)).thenReturn(1510600000)
            Assert.assertTrue(validator.isBlockValidatable(block, previousBlock))

        }

        it("is false when median time past earlier then 2017 November 3, 14:06 GMT") {
            whenever(blockHelper.medianTimePast(block)).thenReturn(1510600000 - 1)
            Assert.assertFalse(validator.isBlockValidatable(block, previousBlock))
        }
    }

    describe("#validate") {
        TODO()
    }

})