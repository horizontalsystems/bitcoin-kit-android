package io.horizontalsystems.bitcoincore.utils

import timber.log.Timber

class HSLogger(private val tag: String) {

    fun v(message: String, vararg args: Any) {
        updateTag()
        Timber.v(message, args)
    }

    fun i(message: String, vararg args: Any) {
        updateTag()
        Timber.i(message, args)
    }

    fun w(message: String, vararg args: Any) {
        updateTag()
        Timber.w(message, args)
    }

    fun w(t: Throwable) {
        updateTag()
        Timber.w(t)
    }

    fun e(t: Throwable, message: String, vararg args: Any) {
        updateTag()
        Timber.e(t, message, args)
    }

    fun e(message: String, vararg args: Any) {
        updateTag()
        Timber.e(message, args)
    }

    fun e(t: Throwable) {
        updateTag()
        Timber.e(t)
    }

    private fun updateTag() {
        Timber.tag(tag)
    }

}
