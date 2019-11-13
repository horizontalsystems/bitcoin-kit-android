package io.horizontalsystems.bitcoincore.managers

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonValue
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
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

}
