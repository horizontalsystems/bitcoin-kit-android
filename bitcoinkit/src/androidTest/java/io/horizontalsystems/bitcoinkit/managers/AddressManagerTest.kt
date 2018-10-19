package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.Mockito.verify

class AddressManagerTest {

    private val factory = RealmFactoryMock()

    private val bloomFilterManager = Mockito.mock(BloomFilterManager::class.java)
    private val hdWallet = Mockito.mock(HDWallet::class.java)
    private val realm = factory.realmFactory.realm
    private val addressConverter = Mockito.mock(AddressConverter::class.java)
    private val addressExternalIndex0 = Mockito.mock(Address::class.java)
    private val addressExternalIndex1 = Mockito.mock(Address::class.java)
    private val addressExternalIndex2 = Mockito.mock(Address::class.java)

    private lateinit var addressManager: AddressManager

    @Before
    fun setup() {
        whenever(addressConverter.convert(byteArrayOf(0))).thenReturn(addressExternalIndex0)
        whenever(addressConverter.convert(byteArrayOf(1))).thenReturn(addressExternalIndex1)
        whenever(addressConverter.convert(byteArrayOf(2))).thenReturn(addressExternalIndex2)

        whenever(addressExternalIndex0.string).thenReturn("external_0")
        whenever(addressExternalIndex1.string).thenReturn("external_1")
        whenever(addressExternalIndex2.string).thenReturn("external_2")

        whenever(hdWallet.hdPublicKey(anyInt(), anyBoolean())).thenAnswer {
            createHDPublicKey(it.getArgument(1), it.getArgument(0))
        }

        addressManager = AddressManager(factory.realmFactory, hdWallet, bloomFilterManager, addressConverter)
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

        assertEquals("external_1", addressManager.receiveAddress())

        verifyNoMoreInteractions(bloomFilterManager)
    }

    @Test
    fun receivePublicKey_empty() {
        assertEquals("external_0", addressManager.receiveAddress())

        val publicKeysReceive = realm.where(PublicKey::class.java).equalTo("external", true).findAll()
        assertTrue(publicKeysReceive.map { it.index }.contains(0))
    }

    @Test
    fun receivePublicKey_allUsed() {
        addUsedReceivePublicKeys(listOf(0, 1))

        assertEquals("external_2", addressManager.receiveAddress())

        val publicKeysReceive = realm.where(PublicKey::class.java).equalTo("external", true).findAll()

        assertTrue(publicKeysReceive.map { it.index }.contains(2))
    }

    @Test
    fun changePublicKey_existingFresh() {
        addFreshChangePublicKeys(listOf(2, 1))

        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 1, actualPublicKey)
        verifyNoMoreInteractions(addressConverter)
    }

    @Test
    fun changePublicKey_empty() {
        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 0, actualPublicKey)

        verify(bloomFilterManager).regenerateBloomFilter()

        val publicKeysChange = realm.where(PublicKey::class.java).equalTo("external", false).findAll()
        assertTrue(publicKeysChange.map { it.index }.contains(0))
    }

    @Test
    fun changePublicKey_allUsed() {
        addUsedChangePublicKeys(listOf(0, 1))

        val actualPublicKey = addressManager.changePublicKey()

        assertPublicKey(false, 2, actualPublicKey)

        verify(bloomFilterManager).regenerateBloomFilter()

        val publicKeysChange = realm.where(PublicKey::class.java).equalTo("external", false).findAll()

        assertTrue(publicKeysChange.map { it.index }.contains(2))
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

        addressManager.fillGap()

        val allExternalPublicKeys = realm.where(PublicKey::class.java)
                .equalTo("external", true)
                .findAll()

        val allInternalPublicKeys = realm.where(PublicKey::class.java)
                .equalTo("external", false)
                .findAll()

        // assertTrue(allExternalPublicKeys.map { it.index }.containsAll(expectedReceivePublicKeyIndexes))
        assertTrue(allInternalPublicKeys.map { it.index }.containsAll(expectedChangePublicKeyIndexes))

        expectedReceivePublicKeyIndexes.forEach { expectedIndex ->
            verify(hdWallet).hdPublicKey(expectedIndex, true)
        }

        expectedChangePublicKeyIndexes.forEach { expectedIndex ->
            verify(hdWallet).hdPublicKey(expectedIndex, false)
        }
    }

    private fun createPublicKey(external: Boolean, index: Int): PublicKey {
        return PublicKey().apply {
            this.external = external
            this.index = index
            this.publicKey = byteArrayOf(index.toByte())
            this.publicKeyHex = "aa$index${if (external) 0 else 1}"
            this.publicKeyHash = byteArrayOf(index.toByte())
        }
    }

    private fun createHDPublicKey(external: Boolean, index: Int): HDPublicKey {
        return HDPublicKey().apply {
            this.external = external
            this.index = index
            this.publicKey = byteArrayOf(index.toByte())
            this.publicKeyHash = byteArrayOf(index.toByte())
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
            keyIndexes.forEach { index ->
                val publicKey = it.copyToRealm(createPublicKey(false, index))

                it.copyToRealm(TransactionOutput().apply {
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
            keyIndexes.forEach { index ->
                val publicKey = it.copyToRealm(createPublicKey(true, index))

                it.copyToRealm(TransactionOutput().apply {
                    this.publicKey = publicKey
                })
            }
        }
    }

    private fun assertPublicKey(external: Boolean, index: Int, publicKey: PublicKey) {
        assertEquals(external, publicKey.external)
        assertEquals(index, publicKey.index)
    }

}
