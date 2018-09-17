package bitcoin.wallet.kit.managers

import io.reactivex.Observable

class ApiManager(url: String) {

    // todo replace stub with implementation
    fun getBlockHashes(address: String): Observable<List<BlockResponse>> {
        return Observable.just(listOf())
    }

}

data class BlockResponse(val hash: String, val height: Int)
