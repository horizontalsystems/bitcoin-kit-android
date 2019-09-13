package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.Utils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object IrregularOutputFinderTest : Spek({

    val block = mock<Block> {
        on { height } doReturn 100
    }

    val output = mock<TransactionOutput> {
        on { index } doReturn 1
        on { transactionHash } doReturn ByteArray(32) { 1 }
    }

    val storage = mock<IStorage> {
        on { lastBlock() } doReturn block
    }

    val irregularScriptTypes = listOf(ScriptType.P2WPKHSH, ScriptType.P2WPKH, ScriptType.P2PK)
    val outputFinder by memoized { IrregularOutputFinder(storage) }

    describe("#getBloomFilterElements") {

        beforeEach {
            whenever(storage.getOutputsForBloomFilter(block.height - 100, irregularScriptTypes)).thenReturn(listOf(output))
        }

        it("returns outputs") {
            val elements = outputFinder.getBloomFilterElements()
            val outpointIndex = Utils.intToByteArray(output.index).reversedArray()

            assertEquals(1, elements.size)
            assertArrayEquals(output.transactionHash + outpointIndex, elements[0])
        }

    }
})
