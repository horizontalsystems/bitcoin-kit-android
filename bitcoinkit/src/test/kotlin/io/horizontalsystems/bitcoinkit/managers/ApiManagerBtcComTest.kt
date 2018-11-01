package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.reactivex.Flowable
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times

class ApiManagerBtcComTest {

    private var publicKey: PublicKey = mock(PublicKey::class.java)
    private val apiRequester = mock(IApiRequester::class.java)
    private val addressSelector = mock(IAddressSelector::class.java)

    private val apiManagerBtcCom = ApiManagerBtcCom(apiRequester, addressSelector)

    @Test
    fun testRequestsBTC() {

        whenever(addressSelector.getAddressVariants(publicKey)).thenReturn(listOf("base58-addr", "publicKeyHex", "pkhsh-addr"))
        whenever(apiRequester.requestTransactions(any(), any())).thenReturn(Flowable.empty())

        apiManagerBtcCom.getBlockHashes(publicKey)

        verify(apiRequester, times(1)).requestTransactions("base58-addr", 1) //base 58
        verify(apiRequester, times(1)).requestTransactions("publicKeyHex", 1) //pkh for segwit
        verify(apiRequester, times(1)).requestTransactions("pkhsh-addr", 1) //p2wpkh (sh)
    }

    @Test
    fun testRequestsBCH() {

        whenever(addressSelector.getAddressVariants(publicKey)).thenReturn(listOf("base58-addr"))
        whenever(apiRequester.requestTransactions(any(), any())).thenReturn(Flowable.empty())

        apiManagerBtcCom.getBlockHashes(publicKey)

        verify(apiRequester, times(1)).requestTransactions("base58-addr", 1) //base 58
        verifyNoMoreInteractions(apiRequester)
    }

    @Test
    fun testSinglePageResponse() {

        val expectedBlockResponse = Array(4) { BlockResponse("block$it", it) }

        whenever(apiRequester.requestTransactions("base58-addr", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(2, 1, 50, listOf(expectedBlockResponse[0], expectedBlockResponse[1]))))

        whenever(apiRequester.requestTransactions("publicKeyHex", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(2, 1, 50, listOf(expectedBlockResponse[1], expectedBlockResponse[2]))))

        whenever(apiRequester.requestTransactions("pkhsh-addr", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(2, 1, 50, listOf(expectedBlockResponse[0], expectedBlockResponse[3]))))


        whenever(addressSelector.getAddressVariants(publicKey)).thenReturn(listOf("base58-addr", "publicKeyHex", "pkhsh-addr"))

        val responseObservable = apiManagerBtcCom.getBlockHashes(publicKey)


        responseObservable.test().assertValue { response ->
            val sortedResponse = response.sortedBy { it.height }.toTypedArray()

            response.isNotEmpty() &&
                    sortedResponse.contentEquals(expectedBlockResponse)
        }
    }

    @Test
    fun testMultiPageResponse() {

        val expectedBlockResponse = Array(7) { BlockResponse("block$it", it) }

        whenever(apiRequester.requestTransactions("base58-addr", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(5, 1, 2, listOf(expectedBlockResponse[0], expectedBlockResponse[1]))))

        whenever(apiRequester.requestTransactions("base58-addr", 2))
                .thenReturn(Flowable.just(ApiAddressTxResponse(5, 2, 2, listOf(expectedBlockResponse[2], expectedBlockResponse[3]))))

        whenever(apiRequester.requestTransactions("base58-addr", 3))
                .thenReturn(Flowable.just(ApiAddressTxResponse(5, 3, 2, listOf(expectedBlockResponse[6]))))

        whenever(apiRequester.requestTransactions("publicKeyHex", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(3, 1, 50, listOf(expectedBlockResponse[4], expectedBlockResponse[3], expectedBlockResponse[5]))))

        whenever(apiRequester.requestTransactions("pkhsh-addr", 1))
                .thenReturn(Flowable.just(ApiAddressTxResponse(2, 1, 50, listOf())))

        whenever(addressSelector.getAddressVariants(publicKey)).thenReturn(listOf("base58-addr", "publicKeyHex", "pkhsh-addr"))

        val responseObservable = apiManagerBtcCom.getBlockHashes(publicKey)

        responseObservable.test().assertValue { response ->
            val sortedResponse = response.sortedBy { it.height }.toTypedArray()

            response.isNotEmpty() &&
                    sortedResponse.contentEquals(expectedBlockResponse)
        }

        verify(apiRequester, times(1)).requestTransactions("base58-addr", 1) //base 58
        verify(apiRequester, times(1)).requestTransactions("base58-addr", 2) //base 58
        verify(apiRequester, times(1)).requestTransactions("base58-addr", 3) //base 58
        verify(apiRequester, times(1)).requestTransactions("publicKeyHex", 1) //pkh for segwit
        verify(apiRequester, times(1)).requestTransactions("pkhsh-addr", 1) //p2wpkh (sh)
    }

    @Test(expected = Exception::class)
    fun testError() {

        whenever(apiRequester.requestTransactions("base58-addr", 1)).thenThrow(Exception())

        apiManagerBtcCom.getBlockHashes(publicKey)
    }

}
