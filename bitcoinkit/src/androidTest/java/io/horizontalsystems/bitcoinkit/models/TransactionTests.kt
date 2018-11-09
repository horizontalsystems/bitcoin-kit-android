package io.horizontalsystems.bitcoinkit.models

import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionTests {
    private val realmFactoryMock = RealmFactoryMock()
    private val realm = realmFactoryMock.realmFactory.realm

    lateinit var transaction: Transaction

    @Before
    fun setup() {
        realm.beginTransaction()
        realm.deleteAll()
        realm.commitTransaction()
    }

    @Test
    fun toSignatureByteArray() {
        realm.beginTransaction()
        val previousTransaction = realm.copyToRealm(Fixtures.transactionP2WPKH)
        realm.commitTransaction()

        val tx = Transaction().apply { version = 1 }
        val payInput = transactionInput(previousTransaction, previousTransaction.outputs[0]!!, byteArrayOf(), 0)
        val payOutput = TransactionOutput().apply {
            value = 9
            index = 0
            lockingScript = "76a914e4de5d630c5cacd7af96418a8f35c411c8ff3c0688ac".hexStringToByteArray()
            scriptType = ScriptType.P2PKH
            keyHash = byteArrayOf()
        }

        tx.inputs.add(payInput)
        tx.outputs.add(payOutput)

        val raw = tx.toSignatureByteArray(0, true)
        assertEquals("010000002d0eeedfe6f0cf82ecf68a1114d4a547e4155e5aeb7134e20280825e88be8f7a8cb9012517c817fead650287d61bdd9c68803b6bf9c64133dcab3e65b5a50cb9f64d1d9f94acbf336c9dd4032815859a6d5081a318eea5f0cb96c39ac1d3d663000000000576a90088ac40aca400000000000000000041a5757ee6b1c0c8b1839c8120d7eb2067b80d10a0090b409e1f6a5d9ca777ea00000000", raw.toHexString())
    }

    private fun transactionInput(previousTransaction: Transaction, prevOutput: TransactionOutput, script: ByteArray, seq: Int): TransactionInput {
        return TransactionInput().apply {
            previousOutputHexReversed = previousTransaction.hashHexReversed
            previousOutputIndex = prevOutput.index.toLong()
            sigScript = script
            sequence = seq.toLong()
            previousOutput = prevOutput
        }
    }
}
