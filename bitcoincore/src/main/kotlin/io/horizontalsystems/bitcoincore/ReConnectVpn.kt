package io.horizontalsystems.bitcoincore

import android.app.Activity
import android.content.Intent
import java.lang.ref.WeakReference

object ReConnectVpn {

    var activity: Activity? = null

    fun reConnectVpn() {
        activity?.sendBroadcast(Intent("com.anwang.safe.reconnect"))
    }
}