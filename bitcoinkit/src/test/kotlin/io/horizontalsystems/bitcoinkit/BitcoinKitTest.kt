package io.horizontalsystems.bitcoinkit

import android.content.Context
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.BitcoinKit.NetworkType
import io.horizontalsystems.bitcoinkit.blocks.BlockSyncer
import io.horizontalsystems.bitcoinkit.core.DataProvider
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BlockDiscoveryBatch
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.managers.ConnectionManager
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.network.peer.PeerHostManager
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.realm.Realm
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(BitcoinKit::class, Realm::class)

class BitcoinKitTest {

    private val context = mock(Context::class.java)
    private val realm = mock(Realm::class.java)
    private val realmFactory = mock(RealmFactory::class.java)
    private val hdWallet = mock(HDWallet::class.java)
    private val mnemonic = mock(Mnemonic::class.java)
    private val peerGroup = mock(PeerGroup::class.java)
    private val network = mock(Network::class.java)
    private val dataProvider = mock(DataProvider::class.java)
    private val peerHostManager = mock(PeerHostManager::class.java)
    private val initialSyncerApi = mock(BlockDiscoveryBatch::class.java)
    private val bloomFilterManager = mock(BloomFilterManager::class.java)
    private val blockSyncer = mock(BlockSyncer::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val connectionManager = mock(ConnectionManager::class.java)

    private val words = listOf("word1", "...", "word12")
    private lateinit var bitcoinKit: BitcoinKit

    @Before
    fun setup() {
        whenNew(ConnectionManager::class.java).thenReturn(connectionManager)
        whenNew(PeerGroup::class.java).thenReturn(peerGroup)
        whenNew(BlockDiscoveryBatch::class.java).thenReturn(initialSyncerApi)
        whenNew(Network::class.java).thenReturn(network)
        whenNew(RealmFactory::class.java).thenReturn(realmFactory)
        whenNew(DataProvider::class.java).thenReturn(dataProvider)
        whenNew(Mnemonic::class.java).thenReturn(mnemonic)
        whenNew(HDWallet::class.java).thenReturn(hdWallet)
        whenNew(BloomFilterManager::class.java).thenReturn(bloomFilterManager)
        whenNew(BlockSyncer::class.java).thenReturn(blockSyncer)
        whenNew(AddressManager::class.java).thenReturn(addressManager)
        whenNew(PeerHostManager::class.java).thenReturn(peerHostManager)

        whenever(realmFactory.realm).thenReturn(realm)
    }

    @Test
    fun mnemonicToSeed() {
        whenever(mnemonic.toSeed(words)).thenReturn(byteArrayOf())
        bitcoinKit = BitcoinKit(context, words, NetworkType.TestNet, "ABC")

        // converts mnemonic words to seed
        verify(mnemonic).toSeed(words)
    }

    @Test
    fun init() {
        PowerMockito.mockStatic(Realm::class.java)

        //assertNull(BitcoinKit.connectionManager)
        BitcoinKit.init(context)
        //assertNotNull(BitcoinKit.connectionManager)
    }

    //  shortcut for PowerMockito#whenNew
    private fun <T> whenNew(clazz: Class<T>) = PowerMockito.whenNew(clazz).withAnyArguments()
}

