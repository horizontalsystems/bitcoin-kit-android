package io.horizontalsystems.bitcoincore.managers

import org.junit.Assert
import org.junit.Test

class BlockHashFetcherHelperTest {

    private val fetcherHelper = BlockHashFetcherHelper()

    @Test
    fun lastUsedIndex_notFound() {
        val addresses = listOf(
                listOf("address0_0", "address0_1"),
                listOf("address1_0", "address1_1")
        )
        val outputs = listOf(
                TransactionOutputItem("asdasd", "asdasd"),
                TransactionOutputItem("tyrty", "sdfasdf")
        )

        val result = fetcherHelper.lastUsedIndex(addresses, outputs)

        Assert.assertEquals(-1, result)
    }

    @Test
    fun lastUsedIndex_foundFirstInAddress() {
        val addresses = listOf(
                listOf("address0_0", "address0_1"),
                listOf("address1_0", "address1_1")
        )
        val outputs = listOf(
                TransactionOutputItem("asdasd", "address0_0"),
                TransactionOutputItem("tyrty", "sdfasdf")
        )

        val result = fetcherHelper.lastUsedIndex(addresses, outputs)

        Assert.assertEquals(0, result)
    }

    @Test
    fun lastUsedIndex_foundSecondInScript() {
        val addresses = listOf(
                listOf("address0_0", "address0_1"),
                listOf("address1_0", "address1_1")
        )
        val outputs = listOf(
                TransactionOutputItem("asdasd", "address0_0"),
                TransactionOutputItem("ssfdaddress1_1aaqqw", "sdfasdf")
        )

        val result = fetcherHelper.lastUsedIndex(addresses, outputs)

        Assert.assertEquals(1, result)
    }
}
