package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoMoreInteractions

class PeerManagerTest {
    private val factories = RealmFactoryMock()
    private val realmFactory = factories.realmFactory

    private val peerDiscover = mock(PeerDiscover::class.java)
    private val network = mock(TestNet::class.java)

    private val ipsPeers = arrayOf("0.0.0.0", "1.1.1.1")
    private val dnsSeeds = arrayOf("abc.com", "com.abc")

    private lateinit var peerManager: PeerManager

    @Before
    fun setup() {
        realmFactory.realm.use { realm ->
            realm.executeTransaction { it.deleteAll() }
            realm.executeTransaction {
                realm.insert(PeerAddress(ipsPeers[0]))
                realm.insert(PeerAddress(ipsPeers[1]))
            }
        }

        whenever(network.dnsSeeds).thenReturn(dnsSeeds)
        whenever(peerDiscover.lookup(dnsSeeds)).thenReturn(ipsPeers)

        peerManager = PeerManager(network, realmFactory, peerDiscover)
    }

    @Test
    fun getPeerIp_withPeerAddresses() {
        var peerIp = peerManager.getPeerIp()

        verifyNoMoreInteractions(peerDiscover)
        assertEquals(ipsPeers[0], peerIp)

        // second time
        peerIp = peerManager.getPeerIp()
        assertEquals(ipsPeers[1], peerIp)
    }

    @Test
    fun getPeerIp_withoutPeerAddresses() {
        // clean peer addresses
        realmFactory.realm.use { realm ->
            realm.executeTransaction { it.deleteAll() }
        }

        val peerIp = peerManager.getPeerIp()

        verify(peerDiscover).lookup(dnsSeeds)
        assertEquals(null, peerIp)
    }

    @Test
    fun markSuccess() {
        val peerIp = ipsPeers[0]
        var peerAddress = getUnusedPeer(realmFactory.realm, peerIp)

        assertEquals(0, peerAddress?.score)
        peerManager.getPeerIp()
        peerManager.markSuccess(peerIp)

        peerAddress = getUnusedPeer(realmFactory.realm, peerIp)
        assertEquals(3, peerAddress?.score)
    }

    @Test
    fun markFailed() {
        val peerIp = ipsPeers[0]
        var peerAddress = getUnusedPeer(realmFactory.realm, peerIp)

        assertTrue(peerAddress != null)

        peerManager.getPeerIp()
        peerManager.markFailed(peerIp)

        peerAddress = getUnusedPeer(realmFactory.realm, peerIp)
        assertTrue(peerAddress == null)
    }

    private fun getUnusedPeer(realm: Realm, peerIp: String): PeerAddress? {
        return realm.where(PeerAddress::class.java)
                .equalTo("ip", peerIp)
                .findFirst()
    }
}
