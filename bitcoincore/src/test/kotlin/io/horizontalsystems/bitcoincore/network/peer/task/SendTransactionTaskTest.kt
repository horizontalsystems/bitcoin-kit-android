package io.horizontalsystems.bitcoincore.network.peer.task

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.models.InventoryItem
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.network.messages.GetDataMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.TransactionMessage
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SendTransactionTaskTest : Spek({

    val fullTransaction = mock<FullTransaction>()
    val task by memoized { SendTransactionTask(fullTransaction) }
    val requester by memoized { mock<PeerTask.Requester>() }
    val listener by memoized { mock<PeerTask.Listener>() }

    beforeEachTest {
        task.requester = requester
        task.listener = listener
    }

    describe("#handleMessage") {

        describe("when message is not GetDataMessage") {
            val message = mock<IMessage>()

            it("is false") {
                Assert.assertFalse(task.handleMessage(message))
            }
        }

        describe("when message is GetDataMessage") {
            val txHash = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            val message = mock<GetDataMessage>()
            val transaction = mock<Transaction>()

            beforeEach {
                whenever(fullTransaction.header).thenReturn(transaction)
                whenever(transaction.hash).thenReturn(txHash)
            }

            describe("when requested this transaction") {
                beforeEach {
                    val inventoryItem = mock<InventoryItem>()
                    whenever(message.inventory).thenReturn(listOf(inventoryItem))
                    whenever(inventoryItem.type).thenReturn(InventoryItem.MSG_TX)
                    whenever(inventoryItem.hash).thenReturn(txHash)
                }

                it("is true") {
                    Assert.assertTrue(task.handleMessage(message))
                }

                it("sends Transaction message and completes itself") {
                    argumentCaptor<IMessage>().apply {
                        task.handleMessage(message)

                        verify(requester).send(capture())
                        Assert.assertEquals(fullTransaction, (firstValue as TransactionMessage).transaction)

                        verify(listener).onTaskCompleted(task)
                    }
                }
            }

            describe("when requested another transaction") {
                beforeEach {
                    val inventoryItem = mock<InventoryItem>()
                    whenever(message.inventory).thenReturn(listOf(inventoryItem))
                    whenever(inventoryItem.type).thenReturn(InventoryItem.MSG_TX)
                    whenever(inventoryItem.hash).thenReturn(txHash.reversedArray())
                }

                it("is false") {
                    Assert.assertFalse(task.handleMessage(message))

                    verifyNoMoreInteractions(requester)
                    verifyNoMoreInteractions(listener)
                }

            }

        }

    }

})
