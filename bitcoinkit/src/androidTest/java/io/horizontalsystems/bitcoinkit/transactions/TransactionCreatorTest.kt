package io.horizontalsystems.bitcoinkit.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionCreatorTest {

    private val realmFactoryMock = RealmFactoryMock()
    private val realm = realmFactoryMock.realmFactory.realm

    private val transactionBuilder = mock(TransactionBuilder::class.java)
    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val peerGroup = mock(PeerGroup::class.java)

    private val transactionCreator = TransactionCreator(realmFactoryMock.realmFactory, transactionBuilder, transactionProcessor, peerGroup)

    @Before
    fun setUp() {
        realm.beginTransaction()
        realm.deleteAll()
        realm.commitTransaction()

        whenever(transactionBuilder.buildTransaction(any(), any(), any(), any(), any())).thenReturn(Fixtures.transactionP2PKH)
    }

    @Test
    fun create_Success() {
        transactionCreator.create("address", 10_000_000)

        val insertedTx = realm.where(Transaction::class.java).equalTo("hashHexReversed", Fixtures.transactionP2PKH.hashHexReversed).findFirst()

        assertTrue(insertedTx != null)
        verify(transactionProcessor).process(Fixtures.transactionP2PKH, realm)
        verify(peerGroup).sendPendingTransactions()
    }

    @Test(expected = TransactionCreator.TransactionAlreadyExists::class)
    fun create_failWithTransactionAlreadyExists() {
        realm.executeTransaction { it.insert(Fixtures.transactionP2PKH) }

        transactionCreator.create("address", 123)
    }
}

