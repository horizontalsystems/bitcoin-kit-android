package io.horizontalsystems.bitcoincore.managers

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.Fixtures
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object UnspentOutputProviderTest : Spek({
    val storage = mock(IStorage::class.java)
    val pluginManager = mock<PluginManager>()

    val output = TransactionOutput(value = 1, index = 0, script = byteArrayOf(), type = ScriptType.P2PKH, keyHash = "000010000".hexToByteArray())
    val pubKey = Fixtures.publicKey
    val lastBlockHeight = 550368
    val blockHeader = BlockHeader(
            version = 1,
            previousBlockHeaderHash = "00000000864b744c5025331036aa4a16e9ed1cbb362908c625272150fa059b29".toReversedByteArray(),
            merkleRoot = "70d6379650ac87eaa4ac1de27c21217b81a034a53abf156c422a538150bd80f4".toReversedByteArray(),
            timestamp = 1337966314,
            bits = 486604799,
            nonce = 2391008772,
            hash = byteArrayOf(1)
    )

    val lastBlock = Block(header = blockHeader, height = lastBlockHeight)
    val confirmationsThreshold = 6

    lateinit var unspentOutput: UnspentOutput

    val transaction by memoized { Transaction() }
    val provider by memoized {
        UnspentOutputProvider(storage = storage, confirmationsThreshold = confirmationsThreshold, pluginManager = pluginManager)
    }

    beforeEachTest {
        whenever(storage.lastBlock()).thenReturn(lastBlock)
    }

    describe("#getSpendableUtxo") {
        context("when transaction is outgoing") {
            beforeEach {
                transaction.isOutgoing = true
                unspentOutput = UnspentOutput(output, pubKey, transaction, null)

                whenever(storage.getUnspentOutputs()).thenReturn(listOf(unspentOutput))
                whenever(pluginManager.isSpendable(unspentOutput)).thenReturn(true)
            }

            it("returns unspentOutput") {
                assertArrayEquals(arrayOf(unspentOutput), provider.getSpendableUtxo().toTypedArray())
            }
        }

        context("when transaction is not outgoing") {
            beforeEach {
                transaction.isOutgoing = false
            }

            context("when transaction is not included in block") {
                beforeEach {
                    unspentOutput = UnspentOutput(output, pubKey, transaction, null)

                    whenever(storage.getUnspentOutputs()).thenReturn(listOf(unspentOutput))
                    whenever(pluginManager.isSpendable(unspentOutput)).thenReturn(true)
                }

                it("doesn't return unspentOutput") {
                    assertArrayEquals(arrayOf(), provider.getSpendableUtxo().toTypedArray())
                }
            }

            context("when transaction is included in block") {
                val block by memoized {
                    Fixtures.block1
                }

                beforeEach {
                    block.height = lastBlockHeight + 1
                    unspentOutput = UnspentOutput(output, pubKey, transaction, block)

                    whenever(storage.getUnspentOutputs()).thenReturn(listOf(unspentOutput))
                    whenever(pluginManager.isSpendable(unspentOutput)).thenReturn(true)
                }

                context("when block has enough confirmations") {
                    it("returns unspentOutput") {
                        block.height = lastBlock.height - confirmationsThreshold

                        assertArrayEquals(arrayOf(unspentOutput), provider.getSpendableUtxo().toTypedArray())
                    }
                }

                context("when block has not enough confirmations") {
                    it("doesn't return unspentOutput") {
                        block.height = lastBlock.height - confirmationsThreshold + 2

                        assertArrayEquals(arrayOf(), provider.getSpendableUtxo().toTypedArray())
                    }
                }
            }
        }
    }

    describe("#balance") {
        beforeEach {
            transaction.isOutgoing = true
        }

        it("returns sum of unspentOutputs") {
            val unspentOutputs = listOf(
                    UnspentOutput(output = output, publicKey = pubKey, transaction = transaction, block = null),
                    UnspentOutput(output = output, publicKey = pubKey, transaction = transaction, block = null)
            )

            whenever(storage.getUnspentOutputs()).thenReturn(unspentOutputs)
            whenever(pluginManager.isSpendable(unspentOutputs[0])).thenReturn(true)
            whenever(pluginManager.isSpendable(unspentOutputs[1])).thenReturn(true)

            val balance = provider.getBalance()

            assertEquals(unspentOutputs[0].output.value + unspentOutputs[1].output.value, balance.spendable)
            assertEquals(0, balance.unspendable)
        }
    }

})
