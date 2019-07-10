package io.horizontalsystems.bitcoincore.utils

import timber.log.Timber

class HSLogger(private val tag: String) {

    fun v(message: String, vararg args: Any) {
        Timber.tag(tag).v(message, *args)
    }

    fun i(message: String, vararg args: Any) {
        Timber.tag(tag).i(message, *args)
    }

    fun w(t: Throwable, message: String, vararg args: Any) {
        Timber.w(t, message, *args)
    }

    fun w(message: String, vararg args: Any) {
        Timber.tag(tag).w(message, *args)
    }

    fun w(t: Throwable) {
        Timber.tag(tag).w(t)
    }

    fun e(t: Throwable, message: String, vararg args: Any) {
        Timber.tag(tag).e(t, message, *args)
    }

    fun e(message: String, vararg args: Any) {
        Timber.tag(tag).e(message, *args)
    }

    fun e(t: Throwable) {
        Timber.tag(tag).e(t)
    }

}
