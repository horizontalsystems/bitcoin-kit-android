package io.horizontalsystems.bitcoincore.managers

import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BlockHashFetcherHelperTest : Spek({

    val fetcherHelper by memoized {
        BlockHashFetcherHelper()
    }

    describe("#lastUsedIndex") {

        it("lastUsedIndex_notFound") {
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

        it("lastUsedIndex_foundFirstInAddress") {
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

        it("lastUsedIndex_foundSecondInScript") {
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

})
