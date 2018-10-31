package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.reactivex.Flowable
import io.reactivex.Single

interface IApiManager {
    fun getBlockHashes(pubKey: PublicKey): Single<List<BlockResponse>>
}

class ApiManagerBtcCom constructor(private val apiRequester: IApiRequester, private val addressSelector: IAddressSelector) : IApiManager {

    override fun getBlockHashes(pubKey: PublicKey): Single<List<BlockResponse>> {
        val addresses = addressSelector.getAddressVariants(pubKey)

        return Flowable.merge(addresses.map { handleRequest(it) })
                .toList()
                .map {
                    it.flatten().distinct()
                }
    }

    private fun handleRequest(address: String, page: Int = 1, resultList: MutableList<BlockResponse> = mutableListOf()): Flowable<List<BlockResponse>> {
        return apiRequester
                .requestTransactions(address, page)
                .flatMap { resp ->
                    resultList.addAll(resp.list)
                    if (resp.totalCount > resp.page * resp.pageSize) {
                        this.handleRequest(address, resp.page + 1, resultList)
                    } else {
                        Flowable.just(resultList)
                    }
                }
    }

}
