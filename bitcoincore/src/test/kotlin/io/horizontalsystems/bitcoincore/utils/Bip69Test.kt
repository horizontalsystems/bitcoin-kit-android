package io.horizontalsystems.bitcoincore.utils

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import org.junit.jupiter.api.Assertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class Bip69Test: Spek({

    describe("sort two outputs") {

        it("sort by amount") {
            val outputWithBigAmount = TransactionOutput().apply {
                keyHash = "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                value = 140
            }
            val outputWithSmallAmount = TransactionOutput().apply {
                keyHash = "76a9145be32612930b8323add2212a4ec03c1562084f8488ac".toByteArray()
                value = 12
            }

            val expected = mutableListOf(outputWithSmallAmount, outputWithBigAmount)
            val list = mutableListOf(outputWithBigAmount, outputWithSmallAmount)
            Collections.sort(list, Bip69.outputComparator)
            Assertions.assertEquals(expected, list)
        }

        it("amount are equal, sort by hashes") {
            val outputHashA = TransactionOutput().apply {
                keyHash = "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                value = 12
            }
            val outputHashB = TransactionOutput().apply {
                keyHash = "76a9145be32612930b8323add2212a4ec03c1562084f8488ac".toByteArray()
                value = 12
            }

            val expected = mutableListOf(outputHashA, outputHashB)
            val list = mutableListOf(outputHashA, outputHashB)
            Collections.sort(list, Bip69.outputComparator)
            Assertions.assertEquals(expected, list)
        }

        it("amount are equal, sort by hashes with different length") {
            val outputHashA = TransactionOutput().apply {
                keyHash = "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                value = 12
            }
            val outputHashB = TransactionOutput().apply {
                keyHash = "76a9144a5fba237213a062f6f57978f7".toByteArray()
                value = 12
            }

            val expected = mutableListOf(outputHashB, outputHashA)
            val list = mutableListOf(outputHashA, outputHashB)
            Collections.sort(list, Bip69.outputComparator)
            Assertions.assertEquals(expected, list)
        }

        it("sort by hashes") {
            val outputHashA = TransactionOutput().apply {
                keyHash = "3d8ed454f4fcc03ba35568aa37528748e56c0142".toByteArray()
                value = 12
            }
            val outputHashB = TransactionOutput().apply {
                keyHash = "e191794cbc83dfaabe399af396904dd22b721ce2".toByteArray()
                value = 12
            }

            val expected = mutableListOf(outputHashA, outputHashB)
            val list = mutableListOf(outputHashB, outputHashA)
            Collections.sort(list, Bip69.outputComparator)
            Assertions.assertEquals(expected, list)
        }
    }

    describe("sort two inputs") {

        it("sort by hash") {
            val unspentOutput1 = mock<UnspentOutput> {
                val transactionOutput = mock<TransactionOutput> {
                    on { transactionHash } doReturn "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                }

                on { output } doReturn transactionOutput
            }

            val unspentOutput2 = mock<UnspentOutput> {
                val transactionOutput = mock<TransactionOutput> {
                    on { transactionHash } doReturn "76a9145be32612930b8323add2212a4ec03c1562084f8488ac".toByteArray()
                }

                on { output } doReturn transactionOutput
            }

            val expected = mutableListOf(unspentOutput1, unspentOutput2)
            val list = mutableListOf(unspentOutput1, unspentOutput2)

            Collections.sort(list, Bip69.inputComparator)

            Assertions.assertEquals(expected, list)
        }

        it("sort by index") {
            val unspentOutput1 = mock<UnspentOutput> {
                val transactionOutput = mock<TransactionOutput> {
                    on { index } doReturn 1
                    on { transactionHash } doReturn "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                }

                on { output } doReturn transactionOutput
            }

            val unspentOutput2 = mock<UnspentOutput> {
                val transactionOutput = mock<TransactionOutput> {
                    on { index } doReturn 0
                    on { transactionHash } doReturn "76a9144a5fba237213a062f6f57978f796390bdcf8d01588ac".toByteArray()
                }

                on { output } doReturn transactionOutput
            }

            val expected = mutableListOf(unspentOutput2, unspentOutput1)
            val list = mutableListOf(unspentOutput1, unspentOutput2)

            Collections.sort(list, Bip69.inputComparator)

            Assertions.assertEquals(expected, list)
        }

    }

})
