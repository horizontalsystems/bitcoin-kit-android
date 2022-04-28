package com.anwang.safewallet.safekit.netwok

import com.anwang.safewallet.safekit.model.SafeNet
import io.reactivex.Single
import retrofit2.http.GET

class SafeProvider(baseUrl: String) {

    private val service by lazy {
        RetrofitUtils.build(baseUrl)
            .create(SafeService::class.java)
    }

    fun getSafeNet(): Single<SafeNet> {
        return service.getSafeNet()
    }

    private interface SafeService {
        @GET("v1/gate/testnet")
        fun getSafeNet(): Single<SafeNet>
    }
}
