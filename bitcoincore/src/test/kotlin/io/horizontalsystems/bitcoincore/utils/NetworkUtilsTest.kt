package io.horizontalsystems.bitcoincore.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.net.URL
import javax.net.ssl.SSLHandshakeException

internal class NetworkUtilsTest {

    // System is  Unable to find valid certification path to below URL
    private val CERT_ERROR_TEST_URL = "https://cashexplorer.bitcoin.com/api/sync/"

    @Test
    fun testUnsafeHttpRequest() {

        assertDoesNotThrow {
            doUnsafeHttpRequest()
        }

        assertThrows(SSLHandshakeException::class.java) { doSafeHttpRequest() }
        assertThrows(SSLHandshakeException::class.java) { doUrlConnectionRequest() }
    }

    private fun doSafeHttpRequest() {
        doOkHttpRequest(OkHttpClient.Builder().build())
    }

    private fun doUnsafeHttpRequest() {
        doOkHttpRequest(NetworkUtils.getUnsafeOkHttpClient())
    }

    private fun doUrlConnectionRequest() {
        URL(CERT_ERROR_TEST_URL)
                .openConnection()
                .apply {
                    connectTimeout = 5000
                    readTimeout = 60000
                    setRequestProperty("Accept", "application/json")
                }.getInputStream()
                .use {
                    //Success
                }
    }

    private fun doOkHttpRequest(httpClient: OkHttpClient) {
        val request = Request.Builder().url(CERT_ERROR_TEST_URL).build()

        return httpClient.newCall(request)
                .execute()
                .use {
                    //success
                }
    }
}