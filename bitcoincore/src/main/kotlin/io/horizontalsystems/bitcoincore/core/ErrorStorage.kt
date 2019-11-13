package io.horizontalsystems.bitcoincore.core

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

class ErrorStorage {
    private val apiErrors = LinkedHashMap<String, Any>()
    private val sendErrors = LinkedHashMap<String, Any>()

    private val utcDateFormatter: DateFormat
    private val dateFormat: String = "MM/dd, HH:mm:ss"

    init {
        utcDateFormatter = SimpleDateFormat(dateFormat, Locale("EN"))
        utcDateFormatter.timeZone = TimeZone.getTimeZone("UTC")
    }

    val errors: Map<String, Any>?
        get() {
            val errors = LinkedHashMap<String, Any>()
            errors.putAll(linkedMapOf("API Errors" to apiErrors))
            errors.putAll(linkedMapOf("Send Errors" to sendErrors))

            return if (errors.count() > 0) errors else null
        }

    fun addApiError(error: Throwable) {
        apiErrors[currentDateTime] = getStackTraceString(error)
    }

    fun addSendError(error: Throwable) {
        sendErrors[currentDateTime] = getStackTraceString(error)
    }

    fun printErrors() {
        for ((key, value) in apiErrors) {
            print("$key: $value")
        }
    }

    private val currentDateTime: String
        get() = utcDateFormatter.format(Date())

    private fun getStackTraceString(error: Throwable): String {
        val sb = StringBuilder()

        sb.appendln(error)

        error.stackTrace.forEachIndexed { index, stackTraceElement ->
            if (index < 5) {
                sb.appendln(stackTraceElement)
            }
        }

        return sb.toString()
    }

}
