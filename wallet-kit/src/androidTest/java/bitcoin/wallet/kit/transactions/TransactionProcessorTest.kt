package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.utils.AddressConverter
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.TestNet
import com.nhaarman.mockito_kotlin.whenever
import io.realm.RealmList
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class TransactionProcessorTest {

    private val factory = RealmFactoryMock()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private val linker = mock(TransactionLinker::class.java)
    private val extractor = mock(TransactionExtractor::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val addressConverter = mock(AddressConverter::class.java)
    private val network = MainNet()

    lateinit var processor: TransactionProcessor

    @Before
    fun setup() {
        processor = TransactionProcessor(realmFactory, addressManager, addressConverter, extractor, linker)
    }

    @After
    fun teardown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun getGapData() {
        val extKey1 = PublicKey().apply {
            external = true
            index = 1
        }

        val extKey2 = PublicKey().apply {
            external = true
            index = 2
        }

        val intKey1 = PublicKey().apply {
            external = false
            index = 1
        }

        val intKey2 = PublicKey().apply {
            external = false
            index = 2
        }

        val transactionMine = Transaction().apply {
            hashHexReversed = "transaction1_1"
            isMine = true
            outputs = RealmList(
                    TransactionOutput().apply { publicKey = extKey2 },
                    TransactionOutput().apply { publicKey = intKey1 }
            )
        }

        val transactionNotMine = Transaction().apply {
            hashHexReversed = "transaction1_2"
            isMine = false
        }

        val transactionMineInOuterBlock = Transaction().apply {
            hashHexReversed = "transaction3_1"
            isMine = true
            outputs = RealmList(
                    TransactionOutput().apply { publicKey = extKey1 },
                    TransactionOutput().apply { publicKey = intKey2 }
            )
        }

        val merkleBlock1 = MerkleBlock().apply {
            header = Header().apply {
                this.prevHash = network.checkpointBlock.headerHash
            }
            associatedTransactions.addAll(listOf(transactionMine, transactionNotMine))
        }

        val merkleBlock2 = MerkleBlock().apply {
            header = Header().apply {
                this.prevHash = merkleBlock1.blockHash
            }
        }


        val merkleBlock3 = MerkleBlock().apply {
            header = Header().apply {
                this.prevHash = merkleBlock2.blockHash
            }
            associatedTransactions.addAll(listOf(transactionMineInOuterBlock))
        }

        val merkleBlocks = listOf(merkleBlock1, merkleBlock2, merkleBlock3)

        whenever(addressManager.gapShiftsOn(extKey1, realm)).thenReturn(false)
        whenever(addressManager.gapShiftsOn(extKey2, realm)).thenReturn(true)

        val gapData = processor.getGapData(merkleBlocks, realm)

        verify(extractor).extract(transactionMine, realm)
        verify(extractor).extract(transactionNotMine, realm)
        verify(extractor).extract(transactionMineInOuterBlock, realm)

        Assert.assertEquals(extKey2, gapData.lastUsedExternalKey)
        Assert.assertEquals(intKey2, gapData.lastUsedInternalKey)
        Assert.assertEquals(merkleBlock1, gapData.firstGapShiftMerkleBlock)
    }


    @Test
    fun run() {
        realm.beginTransaction()

        val transaction1 = realm.copyToRealm(Transaction().apply {
            hashHexReversed = "1"
            processed = true
        })

        val transaction2 = realm.copyToRealm(Transaction().apply {
            hashHexReversed = "2"
            processed = false
        })

        realm.commitTransaction()

        processor.enqueueRun()

        verify(extractor).extract(transaction2, realm)
        verify(extractor, never()).extract(transaction1, realm)

        verify(linker).handle(transaction2, realm)
        verify(linker, never()).handle(transaction1, realm)
        verify(addressManager).fillGap()

        Assert.assertEquals(transaction2.processed, true)
    }

    @Test
    fun run_withoutTransaction() {
        processor.enqueueRun()

        verifyZeroInteractions(extractor)
        verifyZeroInteractions(linker)
        verifyZeroInteractions(addressManager)
    }
}
