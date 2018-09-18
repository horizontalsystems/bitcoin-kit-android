package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.Factories
import bitcoin.wallet.kit.TestUtils.whenever
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.TxOut
import bitcoin.wallet.kit.network.PeerGroup
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify

class AddressManagerTest {

    private val factories = Factories()

    private val peerGroup = Mockito.mock(PeerGroup::class.java)
    private val hdWallet = Mockito.mock(HDWallet::class.java)
    private val realm = factories.realmFactory.realm

    private lateinit var addressManager: AddressManager

    @Before
    fun setup() {
        whenever(hdWallet.receivePublicKey(ArgumentMatchers.anyInt())).thenAnswer {
            createPublicKey(true, it.getArgument(0))
        }
        whenever(hdWallet.changePublicKey(ArgumentMatchers.anyInt())).thenAnswer {
            createPublicKey(false, it.getArgument(0))
        }

        addressManager = AddressManager(factories.realmFactory, hdWallet, peerGroup)
    }

    @After
    fun tearDown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun receivePublicKey_existingFresh() {
        addFreshReceivePublicKeys(listOf(2, 1))

        Assert.assertEquals("external_1", addressManager.receiveAddress())

        verifyNoMoreInteractions(peerGroup)
    }

    @Test
    fun receivePublicKey_empty() {
        Assert.assertEquals("external_0", addressManager.receiveAddress())

        verify(peerGroup).addPublicKeyFilter(check {
            assertPublicKey(true, 0, it)
        })

        val publicKeysReceive = realm.where(PublicKey::class.java).equalTo("external", true).findAll()
        Assert.assertTrue(publicKeysReceive.map { it.index }.contains(0))
    }

    @Test
    fun receivePublicKey_allUsed() {
        addUsedReceivePublicKeys(listOf(0, 1))

        Assert.assertEquals("external_2", addressManager.receiveAddress())

        verify(peerGroup).addPublicKeyFilter(check {
            assertPublicKey(true, 2, it)
        })

        val publicKeysReceive = realm.where(PublicKey::class.java).equalTo("external", true).findAll()

        Assert.assertTrue(publicKeysReceive.map { it.index }.contains(2))
    }

    @Test
    fun changePublicKey_existingFresh() {
        addFreshChangePublicKeys(listOf(2, 1))

        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 1, actualPublicKey)
        verifyNoMoreInteractions(peerGroup)
    }

    @Test
    fun changePublicKey_empty() {
        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 0, actualPublicKey)

        verify(peerGroup).addPublicKeyFilter(check {
            assertPublicKey(false, 0, it)
        })

        val publicKeysChange = realm.where(PublicKey::class.java).equalTo("external", false).findAll()
        Assert.assertTrue(publicKeysChange.map { it.index }.contains(0))

    }

    @Test
    fun changePublicKey_allUsed() {
        addUsedChangePublicKeys(listOf(0, 1))

        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 2, actualPublicKey)

        verify(peerGroup).addPublicKeyFilter(check {
            assertPublicKey(false, 2, it)
        })

        val publicKeysChange = realm.where(PublicKey::class.java).equalTo("external", false).findAll()

        Assert.assertTrue(publicKeysChange.map { it.index }.contains(2))
    }

    @Test
    fun generateKeys() {
        val gapLimit = 5

        // indexes of receive public keys that are already used
        addUsedReceivePublicKeys(listOf(0, 2))
        // indexes of receive public keys that are generated but never used
        addFreshReceivePublicKeys(listOf(1, 3))

        // expected list of indexes of receive public keys that will be generated
        val expectedReceivePublicKeyIndexes = listOf(4, 5, 6)
        // expected list of indexes of change public keys that will be generated
        val expectedChangePublicKeyIndexes = listOf(0, 1, 2, 3, 4)

        whenever(hdWallet.gapLimit).thenReturn(gapLimit)

        addressManager.generateKeys()

        val allExternalPublicKeys = realm.where(PublicKey::class.java)
                .equalTo("external", true)
                .findAll()

        val allInternalPublicKeys = realm.where(PublicKey::class.java)
                .equalTo("external", false)
                .findAll()

        Assert.assertTrue(allExternalPublicKeys.map { it.index }.containsAll(expectedReceivePublicKeyIndexes))
        Assert.assertTrue(allInternalPublicKeys.map { it.index }.containsAll(expectedChangePublicKeyIndexes))

        expectedReceivePublicKeyIndexes.forEach { expectedIndex ->
            verify(hdWallet).receivePublicKey(expectedIndex)
            verify(peerGroup).addPublicKeyFilter(argThat { this.external && this.index == expectedIndex })
        }

        expectedChangePublicKeyIndexes.forEach { expectedIndex ->
            verify(hdWallet).changePublicKey(expectedIndex)
            verify(peerGroup).addPublicKeyFilter(argThat { !this.external && this.index == expectedIndex })
        }
    }

    private fun createPublicKey(external: Boolean, index: Int): PublicKey {
        return PublicKey().apply {
            this.external = external
            this.index = index
            address = "${if (external) "external" else "internal"}_$index"
        }
    }

    private fun addFreshChangePublicKeys(keyIndexes: List<Int>) {
        realm.executeTransaction {
            keyIndexes.forEach { index ->
                it.insert(createPublicKey(false, index))
            }
        }
    }

    private fun addUsedChangePublicKeys(keyIndexes: List<Int>) {
        realm.executeTransaction {
            keyIndexes.forEach {index ->
                val publicKey = it.copyToRealm(createPublicKey(false, index))

                it.copyToRealm(TxOut().apply {
                    this.publicKey = publicKey
                })
            }
        }
    }

    private fun addFreshReceivePublicKeys(keyIndexes: List<Int>) {
        realm.executeTransaction {
            keyIndexes.forEach { index ->
                it.insert(createPublicKey(true, index))
            }
        }
    }

    private fun addUsedReceivePublicKeys(keyIndexes: List<Int>) {
        realm.executeTransaction {
            keyIndexes.forEach {index ->
                val publicKey = it.copyToRealm(createPublicKey(true, index))

                it.copyToRealm(TxOut().apply {
                    this.publicKey = publicKey
                })
            }
        }
    }

    private fun assertPublicKey(external: Boolean, index: Int, publicKey: PublicKey) {
        Assert.assertEquals(external, publicKey.external)
        Assert.assertEquals(index, publicKey.index)
    }

}
