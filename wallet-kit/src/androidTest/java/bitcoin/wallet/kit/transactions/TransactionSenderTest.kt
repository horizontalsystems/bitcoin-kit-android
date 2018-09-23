package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.PeerGroup
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TransactionSenderTest {

    private val factory = RealmFactoryMock()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val peerGroup = mock(PeerGroup::class.java)

    lateinit var sender: TransactionSender

    @Before
    fun setup() {
        sender = TransactionSender(realmFactory, peerGroup)
    }

    @After
    fun tearDown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun run_relayOnlyNewTransactions() {

        realm.beginTransaction()
        realm.insert(Transaction().apply { reversedHashHex = "1" })
        realm.insert(Transaction().apply {
            reversedHashHex = "2"
            status = Transaction.Status.INVALID
        })

        val transaction = realm.copyToRealm(Transaction().apply {
            reversedHashHex = "3"
            status = Transaction.Status.NEW
        })

        realm.commitTransaction()

        sender.run()

        verify(peerGroup).relay(transaction!!)
        verifyNoMoreInteractions(peerGroup)
    }
}
