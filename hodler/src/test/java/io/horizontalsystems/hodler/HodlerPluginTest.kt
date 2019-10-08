package io.horizontalsystems.hodler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import org.junit.Test

class HodlerPluginTest {

    @Test
    fun processTransactionWithNullData() {
        val plugin = HodlerPlugin()

        val fullTransaction = mock<FullTransaction>()
        val nullDataOutput = mock<TransactionOutput>()
        val storage = mock<IStorage>()
        val publicKey = mock<PublicKey>()
        val publicKeyPath = "publicKeyPath"
        val transactionOutputPayment = mock<TransactionOutput>()
        val transactionOutputData = mock<TransactionOutput>()
        val transaction = mock<Transaction>()

        whenever(storage.getPublicKeyByKeyOrKeyHash(any())).thenReturn(publicKey)
        whenever(nullDataOutput.lockingScript).thenReturn("6a510468e09a5d14bff7529bed62cb3a2ba0d42b9fd807a1dfc78801".hexToByteArray())
        whenever(fullTransaction.outputs).thenReturn(listOf(transactionOutputPayment, transactionOutputData))
        whenever(fullTransaction.header).thenReturn(transaction)
        whenever(transactionOutputPayment.keyHash).thenReturn("fe0762dbf54a9bb98acacf550b274a6f3c517c4a".hexToByteArray())
        whenever(publicKey.path).thenReturn(publicKeyPath)


        plugin.processTransactionWithNullData(fullTransaction, nullDataOutput, storage)

        verify(transactionOutputPayment).redeemScript = "0468e09a5db17576a914bff7529bed62cb3a2ba0d42b9fd807a1dfc7880188ac".hexToByteArray()
        verify(transactionOutputPayment).publicKeyPath = publicKeyPath
        verify(transaction).isMine = true
    }
}