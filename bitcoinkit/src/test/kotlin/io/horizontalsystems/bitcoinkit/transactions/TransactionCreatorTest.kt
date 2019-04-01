package io.horizontalsystems.bitcoinkit.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.Fixtures
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class TransactionCreatorTest {
    private val transactionBuilder = Mockito.mock(TransactionBuilder::class.java)
    private val transactionProcessor = Mockito.mock(TransactionProcessor::class.java)
    private val peerGroup = Mockito.mock(PeerGroup::class.java)

    private val transactionP2PKH = Fixtures.transactionP2PKH
    private val transactionCreator = TransactionCreator(transactionBuilder, transactionProcessor, peerGroup)

    @Before
    fun setUp() {
        whenever(transactionBuilder.buildTransaction(any(), any(), any(), any())).thenReturn(transactionP2PKH)
    }

    @Test
    fun create_Success() {
        transactionCreator.create("address", 10_000_000, 8, true)

        verify(transactionProcessor).processOutgoing(transactionP2PKH)
        verify(peerGroup).sendPendingTransactions()
    }
}

