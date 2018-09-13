package bitcoin.wallet.kit.managers

import io.reactivex.Observable

class ApiManager {

    fun getBlockHashes(address: String): Observable<List<BlockResponse>> {
        TODO("not implemented")
    }

}

data class BlockResponse(val hash: String, val height: Int)
