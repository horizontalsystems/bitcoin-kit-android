package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class ApiManager(private val host: String) {
    private val logger = Logger.getLogger("ApiManager")

    @Throws
    fun get(resource: String): JsonValue {
        val url = "$host/$resource"

        logger.info("Fetching $url")

        return try {
            URL(url)
                    .openConnection()
                    .apply {
                        connectTimeout = 5000
                        readTimeout = 60000
                        setRequestProperty("Accept", "application/json")
                    }.getInputStream()
                    .use {
                        Json.parse(it.bufferedReader())
                    }
        } catch (exception: IOException) {
            throw Exception("${exception.javaClass.simpleName}: $host")
        }
    }

    @Throws
    fun post(resource: String, data: String): JsonValue {
        try {
            val path = "$host/$resource"

            logger.info("Fetching $path")

            val url = URL(path)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
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
            throw Exception("${exception.javaClass.simpleName}: $host")
        }
    }

    fun doOkHttpGet(safeCall: Boolean, uri: String): JsonValue {

        val url = "$host/$uri"

        try {
            val httpClient: OkHttpClient = if (!safeCall)
                NetworkUtils.getUnsafeOkHttpClient()
            else {
                OkHttpClient.Builder()
                        .apply {
                            connectTimeout(5000, TimeUnit.MILLISECONDS)
                            readTimeout(60000, TimeUnit.MILLISECONDS)
                        }.build()
            }

            httpClient.newCall(Request.Builder().url(url).build())
                    .execute()
                    .use { response ->

                        if (response.isSuccessful) {
                            response.body?.let {
                                return Json.parse(it.string())
                            }
                        }

                        throw IOException("Unexpected Error:$response")
                    }
        } catch (e: Exception) {
            throw Exception("${e.javaClass.simpleName}: $host, ${e.localizedMessage}")
        }
    }
}
