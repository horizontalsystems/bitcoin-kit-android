package com.anwang.safewallet.safekit

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.logging.Logger

class MainSafeNetService(val context: Context, val mainNetSafe: MainNetSafe) {

    private val logger = Logger.getLogger("MainSafeNetService")
    private val service: SafeNetServiceApi
    private val gson: Gson

    private val url: String = "https://chain.anwang.org/"
    private val sp = context.getSharedPreferences("MainSafeNet", Context.MODE_PRIVATE)

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = NetworkUtils.getUnsafeOkHttpClient().newBuilder()
            .addInterceptor(loggingInterceptor)

        gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create(SafeNetServiceApi::class.java)

        getSeed()
    }


    private fun getSeed() {
        service.getSeed().map { map ->
            var result = parseResponse<String>(map)
            if (result.isNullOrEmpty()) {
                val cache = sp.getStringSet("seedIp", null)
                cache?.let {
                    result = it.toList()
                }
            } else {
                sp.edit().putStringSet("seedIp", result.toSet()).commit()
            }
            result
        }
            .subscribeOn(Schedulers.io())
            .subscribe({
                mainNetSafe.dnsSeeds = it
            }, {
                val cache = sp.getStringSet("seedIp", null)
                cache?.let {
                    if (it.isNotEmpty()) mainNetSafe.dnsSeeds = it.toList()
                }
            }).let {

            }
    }

    private fun <T> parseResponse(response: JsonElement): List<T> {
        try {
            val jsonArray = response.asJsonArray
            val result: List<T> = gson.fromJson(jsonArray, object : TypeToken<List<T>>() {}.type)
            return result
        }  catch (err: Throwable) {

        }
        return emptyList()
    }

    private interface SafeNetServiceApi {
        @GET("/insight-api-safe/utils/address/seed")
        fun getSeed(): Single<JsonElement>
    }

}