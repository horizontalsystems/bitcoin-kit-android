package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
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
                    }.getInputStream()
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

                        throw IOException("Unexpected Error:$response")
                    }
        } catch (e: Exception) {
            throw Exception("${e.javaClass.simpleName}: $host, ${e.localizedMessage}")
        }
    }
}
