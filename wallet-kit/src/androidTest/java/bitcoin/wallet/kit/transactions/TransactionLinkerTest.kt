package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionInput
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType
import helpers.Fixtures
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionLinkerTest {

    private val factory = RealmFactoryMock()
    private lateinit var realm: Realm

    private val linker = TransactionLinker()
    private var pubKeyHash = "1ec865abcb88cec71c484d4dadec3d7dc0271a7b".toByteArray()

    private lateinit var transactionP2PK: Transaction
    private lateinit var pubKey: PublicKey

    @Before
    fun setup() {
        realm = factory.realmFactory.realm
        realm.beginTransaction()
        realm.deleteAll()

        transactionP2PK = realm.copyToRealm(Fixtures.transactionP2PK)
        pubKey = realm.copyToRealm(PublicKey().apply {
            publicKeyHash = pubKeyHash
            address = "Rsfz3aRmCwTe2J8pSWSYRNYmweJ"
        })

        realm.commitTransaction()
    }

    @Test
    fun listOutputs() {
        val savedNextTransaction = savedNextTx()
        realm.executeTransaction { it.insert(savedNextTransaction) }

        assertEquals(savedNextTransaction.inputs[0]?.previousOutput, null)
        linker.handle(savedNextTransaction, realm)

        assertOutputEqual(
                savedNextTransaction.inputs[0]!!.previousOutput!!,
                transactionP2PK.outputs[0]!!
        )
    }

    @Test
    fun listInputs() {
        realm.beginTransaction()

        val savedPreviousTransaction = realm.copyToRealm(Transaction().apply {
            hashHexReversed = transactionP2PK.inputs[0]!!.previousOutputHexReversed
            outputs.add(TransactionOutput().apply {
                index = transactionP2PK.inputs[0]!!.previousOutputIndex.toInt()
                value = 100000
            })
        })

        realm.commitTransaction()

        assertEquals(transactionP2PK.inputs[0]!!.previousOutput, null)
        realm.executeTransaction { linker.handle(savedPreviousTransaction, it) }
        assertOutputEqual(
                transactionP2PK.inputs[0]!!.previousOutput!!,
                savedPreviousTransaction.outputs[0]!!
        )
    }

    @Test
    fun transactionAndOutput_isMine() {
        realm.executeTransaction {
            transactionP2PK.outputs[0]!!.scriptType = ScriptType.P2PKH
            transactionP2PK.outputs[0]!!.keyHash = pubKeyHash
        }

        assertEquals(transactionP2PK.isMine, false)
        assertEquals(transactionP2PK.outputs[0]!!.publicKey, null)

        realm.executeTransaction { linker.handle(transactionP2PK, it) }
        assertEquals(transactionP2PK.isMine, true)
        assertEquals(transactionP2PK.outputs[0]!!.publicKey, pubKey)
    }

    @Test
    fun dontSetTransactionAndOutputIsMine() {
        realm.executeTransaction {
            transactionP2PK.outputs[0]!!.scriptType = ScriptType.P2PKH
        }

        assertEquals(transactionP2PK.isMine, false)
        assertEquals(transactionP2PK.outputs[0]!!.publicKey, null)
        realm.executeTransaction { linker.handle(transactionP2PK, it) }
        assertEquals(transactionP2PK.isMine, false)
        assertEquals(transactionP2PK.outputs[0]!!.publicKey, null)
    }

    @Test
    fun setNextTransactionIsMine() {
        realm.beginTransaction()

        val savedNextTransaction = realm.copyToRealm(savedNextTx())

        transactionP2PK.outputs[0]!!.scriptType = ScriptType.P2PKH
        transactionP2PK.outputs[0]!!.keyHash = pubKeyHash
        realm.commitTransaction()

        assertEquals(savedNextTransaction.isMine, false)
        realm.executeTransaction { linker.handle(transactionP2PK, it) }
        assertEquals(savedNextTransaction.isMine, true)
    }

    @Test
    fun dontSetNextTransactionIsMine() {
        realm.beginTransaction()

        val savedNextTransaction = realm.copyToRealm(savedNextTx())
        transactionP2PK.outputs[0]!!.scriptType = ScriptType.P2PKH
        realm.commitTransaction()

        assertEquals(savedNextTransaction.isMine, false)
        realm.executeTransaction { linker.handle(transactionP2PK, it) }
        assertEquals(savedNextTransaction.isMine, false)
    }

    private fun savedNextTx(): Transaction {
        return Transaction().apply {
            hashHexReversed = "0000000000000000000111111111111122222222222222333333333333333000"
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = transactionP2PK.hashHexReversed
                previousOutputIndex = transactionP2PK.outputs[0]!!.index.toLong()
                sequence = 100
            })
        }
    }

    private fun assertOutputEqual(out1: TransactionOutput, out2: TransactionOutput) {
        assertEquals(out1.value, out2.value)
        assertEquals(out1.lockingScript.toHexString(), out2.lockingScript.toHexString())
        assertEquals(out1.index, out2.index)
    }
}
