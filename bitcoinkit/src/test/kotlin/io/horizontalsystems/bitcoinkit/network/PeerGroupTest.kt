package io.horizontalsystems.bitcoinkit.network

import android.content.Context
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.internal.RealmCore
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.SocketTimeoutException

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroup::class, Realm::class, RealmConfiguration::class, RealmCore::class)

class PeerGroupTest {
    private lateinit var peerGroup: PeerGroup
    private lateinit var peer: Peer
    private lateinit var peer2: Peer
    private lateinit var peerManager: PeerManager
    private lateinit var bloomFilterManager: BloomFilterManager
    private val peerIp = "8.8.8.8"
    private val peerIp2 = "5.5.5.5"
    private val network = MainNet()

    @Before
    fun setup() {
        peerManager = mock(PeerManager::class.java)
        bloomFilterManager = mock(BloomFilterManager::class.java)
        peerGroup = PeerGroup(peerManager, bloomFilterManager, network, 2)
        peer = mock(Peer::class.java)
        peer2 = mock(Peer::class.java)
        whenever(peer.host).thenReturn(peerIp)
        whenever(peer2.host).thenReturn(peerIp2)

        whenever(peerManager.getPeerIp())
                .thenReturn(peerIp, peerIp2)

        PowerMockito.whenNew(Peer::class.java)
                .withAnyArguments()
                .thenReturn(peer, peer2)

        // Realm initialize
        mockStatic(Realm::class.java)
        mockStatic(RealmConfiguration::class.java)
        mockStatic(RealmCore::class.java)

        RealmCore.loadLibrary(any(Context::class.java))
    }

    @Test
    fun run() { // creates peer connection with given IP address
        peerGroup.start()

        Thread.sleep(500L)
        verify(peer).start()

        // close thread:
        peerGroup.close()
        peerGroup.join()
    }


    @Test
    fun disconnected_withError() { // removes peer from connection list
        peerGroup.disconnected(peer, SocketTimeoutException("Some Error"))

        verify(peerManager).markFailed(peerIp)
    }
}
