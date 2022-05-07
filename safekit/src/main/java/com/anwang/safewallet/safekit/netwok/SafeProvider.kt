package com.anwang.safewallet.safekit.netwok

import com.anwang.safewallet.safekit.model.SafeInfo
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

class SafeProvider(baseUrl: String) {

    private val service by lazy {
        RetrofitUtils.build(baseUrl)
            .create(SafeService::class.java)
    }

    fun getSafeInfo(netType: String): Single<SafeInfo> {
        return service.getSafeInfo(netType)
    }

    private interface SafeService {
        @GET("v1/gate/{netType}")
        fun getSafeInfo(@Path("netType")netType: String): Single<SafeInfo>
    }
}
