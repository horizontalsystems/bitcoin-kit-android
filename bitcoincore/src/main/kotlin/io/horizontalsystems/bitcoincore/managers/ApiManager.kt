package io.horizontalsystems.bitcoincore.managers

import android.util.Log
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class ApiManager(private val host: String) {
    private val logger = Logger.getLogger("ApiManager")

    @Throws
    fun get(resource: String): InputStream? {
        val url = "$host/$resource"

        logger.info("Fetching $url")

        return try {
            URL(url)
                .openConnection()
                .apply {
                    connectTimeout = 5000
                    readTimeout = 60000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("content-type", "application/json")
                }.getInputStream()
        } catch (exception: IOException) {
            throw ApiManagerException.Other("${exception.javaClass.simpleName}: $host")
        }
    }

    @Throws
    fun post(resource: String, data: String): JsonValue {
        try {
            val path = "$host/$resource"

            Log.e("e", "path= $path, data= $data")

            logger.info("Fetching $path")

            val url = URL(path)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            val out = BufferedOutputStream(urlConnection.outputStream)
            val writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
            writer.write(data)
            writer.flush()
            writer.close()
            out.close()

            return urlConnection.inputStream.use {
                Json.parse(it.bufferedReader())
            }
        } catch (exception: IOException) {
            throw ApiManagerException.Other("${exception.javaClass.simpleName}: $host")
        }
    }

    fun doOkHttpGet(uri: String): JsonValue {

        val url = "$host/$uri"

        try {
            val httpClient: OkHttpClient = OkHttpClient.Builder()
                .apply {
                    connectTimeout(5000, TimeUnit.MILLISECONDS)
                    readTimeout(60000, TimeUnit.MILLISECONDS)
                }.build()

            httpClient.newCall(Request.Builder().url(url).build())
                .execute()
                .use { response ->

                    if (response.isSuccessful) {
                        response.body?.let {
                            return Json.parse(it.string())
                        }
                    }

                    if (response.code == 404) {
                        throw ApiManagerException.Http404Exception
                    } else {
                        throw ApiManagerException.Other("Unexpected Error:$response")
                    }
                }
        } catch (e: ApiManagerException) {
            throw e
        } catch (e: Exception) {
            throw ApiManagerException.Other("${e.javaClass.simpleName}: $host, ${e.localizedMessage}")
        }
    }

}

sealed class ApiManagerException : Exception() {
    object Http404Exception : ApiManagerException()
    class Other(override val message: String) : ApiManagerException()
}
