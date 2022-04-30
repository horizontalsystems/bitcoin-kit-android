package com.anwang.safewallet.safekit.netwok

import com.anwang.safewallet.safekit.model.SafeInfo
import io.reactivex.Single
import retrofit2.http.GET

class SafeProvider(baseUrl: String) {

    private val service by lazy {
        RetrofitUtils.build(baseUrl)
            .create(SafeService::class.java)
    }

    fun getSafeInfo(): Single<SafeInfo> {
        return service.getSafeInfo()
    }

    private interface SafeService {
        @GET("v1/gate/testnet")
        fun getSafeInfo(): Single<SafeInfo>
    }
}
