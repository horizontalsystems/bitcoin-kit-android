package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionInput
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType
import helpers.Fixtures
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

class UnspentOutputProviderTest {

    private val factory = RealmFactoryMock()
    private lateinit var realm: Realm

    private lateinit var unspentOutputProvider: UnspentOutputProvider

    private lateinit var unspentOutputs: List<TransactionOutput>

    @Before
    fun setUp() {
        unspentOutputProvider = UnspentOutputProvider(factory.realmFactory)

        unspentOutputs = listOf(
                TransactionOutput().apply { value = 1; scriptType = ScriptType.P2PK; keyHash = "00010000".hexStringToByteArray() },
                TransactionOutput().apply { value = 2; scriptType = ScriptType.P2PKH; keyHash = "00010001".hexStringToByteArray() },
                TransactionOutput().apply { value = 3; scriptType = ScriptType.P2PKH; keyHash = "00010002".hexStringToByteArray() },
                TransactionOutput().apply { value = 4; scriptType = ScriptType.P2PKH; keyHash = "00010004".hexStringToByteArray() },
                TransactionOutput().apply { value = 5; scriptType = ScriptType.P2SH; keyHash = "00010005".hexStringToByteArray() })

        realm = factory.realmFactory.realm

        realm.executeTransaction { it.deleteAll() }
    }

    @Test
    fun allUnspentOutputs_allPublicKeysNull() {

        val unspents = unspentOutputProvider.allUnspentOutputs()

        Assert.assertEquals(0, unspents.size)
    }

    @Test
    fun allUnspentOutputs_oneSpent() {

        unspentOutputs.forEach { it.publicKey = Fixtures.publicKey }

        realm.beginTransaction()

        val incomingTx = realm.copyToRealm(
                Transaction(1, 0).apply {
                    outputs.addAll(unspentOutputs)
                    setHashes()
                })

        val outGoingTx = realm.copyToRealm(Transaction(1, 0).apply {
            inputs.add(TransactionInput().apply {
                previousOutputHexReversed = incomingTx.hashHexReversed
                previousOutput = incomingTx.outputs[0]
            })

            setHashes()
        })

        realm.commitTransaction()

        val unspents = unspentOutputProvider.allUnspentOutputs()

        Assert.assertEquals(3, unspents.size)

        Assert.assertEquals(unspents[0].keyHash?.toHexString(), unspentOutputs[1].keyHash?.toHexString())
        Assert.assertEquals(unspents[1].keyHash?.toHexString(), unspentOutputs[2].keyHash?.toHexString())
        Assert.assertEquals(unspents[2].keyHash?.toHexString(), unspentOutputs[3].keyHash?.toHexString())
    }

    @Test
    fun allUnspentOutputs_allSpent() {

        unspentOutputs.forEach { it.publicKey = Fixtures.publicKey }

        realm.beginTransaction()

        val incomingTx = realm.copyToRealm(
                Transaction(1, 0).apply {
                    outputs.addAll(unspentOutputs)
                    setHashes()
                })

        val outGoingTx = realm.copyToRealm(Transaction(1, 0).apply {
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
            }
            ))

            setHashes()
        })

        realm.commitTransaction()

        val unspents = unspentOutputProvider.allUnspentOutputs()

        Assert.assertEquals(0, unspents.size)
    }

}
