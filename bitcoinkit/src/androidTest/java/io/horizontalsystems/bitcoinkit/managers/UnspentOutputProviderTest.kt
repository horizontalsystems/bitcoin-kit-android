package io.horizontalsystems.bitcoinkit.managers

import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2PK
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2PKH
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.P2SH
import io.horizontalsystems.bitcoinkit.scripts.ScriptType.UNKNOWN
import io.realm.Realm
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UnspentOutputProviderTest {

    private val factories = RealmFactoryMock()
    private val realmFactory = factories.realmFactory

    private lateinit var realm: Realm
    private lateinit var unspentOutputProvider: UnspentOutputProvider
    private lateinit var unspentOutputs: List<TransactionOutput>

    @Before
    fun setUp() {
        unspentOutputProvider = UnspentOutputProvider(realmFactory)
        unspentOutputs = listOf(
                TransactionOutput().apply { value = 1; scriptType = P2PK; keyHash = "00010000".hexStringToByteArray() },
                TransactionOutput().apply { value = 2; scriptType = P2PKH; keyHash = "00010001".hexStringToByteArray() },
                TransactionOutput().apply { value = 3; scriptType = P2PKH; keyHash = "00010002".hexStringToByteArray() },
                TransactionOutput().apply { value = 4; scriptType = P2PKH; keyHash = "00010004".hexStringToByteArray() },
                TransactionOutput().apply { value = 5; scriptType = P2SH; keyHash = "00010005".hexStringToByteArray() },
                TransactionOutput().apply { value = 6; scriptType = UNKNOWN; keyHash = "00000".hexStringToByteArray() })

        realm = realmFactory.realm
        realm.executeTransaction { it.deleteAll() }
    }

    @Test
    fun allUnspentOutputs_allPublicKeysNull() {
        assertEquals(0, unspentOutputProvider.allUnspentOutputs().size)
    }

    @Test
    fun allUnspentOutputs_oneSpent() {
        unspentOutputs.forEach { it.publicKey = Fixtures.publicKey }

        realm.executeTransaction {
            val incomingTx = realm.copyToRealm(Transaction(1, 0).apply {
                outputs.addAll(unspentOutputs)
                setHashes()
            })

            realm.copyToRealm(Transaction(1, 0).apply {
                inputs.add(TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[0]
                })
                setHashes()
            })
        }

        val unspents = unspentOutputProvider.allUnspentOutputs()

        assertEquals(4, unspents.size)

        assertEquals(unspents[0].keyHash?.toHexString(), unspentOutputs[1].keyHash?.toHexString())
        assertEquals(unspents[1].keyHash?.toHexString(), unspentOutputs[2].keyHash?.toHexString())
        assertEquals(unspents[2].keyHash?.toHexString(), unspentOutputs[3].keyHash?.toHexString())
        assertEquals(unspents[3].keyHash?.toHexString(), unspentOutputs[4].keyHash?.toHexString())
    }

    @Test
    fun allUnspentOutputs_noLinkedInputs() { // one spent but no linked inputs
        unspentOutputs.forEach { it.publicKey = Fixtures.publicKey }
        realm.executeTransaction {
            it.copyToRealm(Transaction(1, 0).apply {
                outputs.addAll(unspentOutputs)
                setHashes()
            })
        }

        assertEquals(5, unspentOutputProvider.allUnspentOutputs().size)
    }

    @Test
    fun allUnspentOutputs_allSpent() {
        unspentOutputs.forEach { it.publicKey = Fixtures.publicKey }

        realm.executeTransaction {
            val incomingTx = realm.copyToRealm(Transaction(1, 0).apply {
                outputs.addAll(unspentOutputs)
                setHashes()
            })

            realm.copyToRealm(Transaction(1, 0).apply {
                inputs.addAll(listOf(TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[0]
                }, TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[1]
                }, TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[2]
                }, TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[3]
                }, TransactionInput().apply {
                    previousOutputHexReversed = incomingTx.hashHexReversed
                    previousOutput = incomingTx.outputs[4]
                }))

                setHashes()
            })
        }

        assertEquals(0, unspentOutputProvider.allUnspentOutputs().size)
    }
}
