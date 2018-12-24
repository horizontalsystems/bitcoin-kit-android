package io.horizontalsystems.bitcoinkit.managers

import android.content.Context
import android.net.ConnectivityManager

class ConnectionManager(context: Context) {
    val isOnline: Boolean
        get() = connectivityManager.activeNetworkInfo?.isConnected ?: false

    private var connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}
