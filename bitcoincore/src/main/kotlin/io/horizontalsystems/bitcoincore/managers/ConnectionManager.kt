package io.horizontalsystems.bitcoincore.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import java.util.concurrent.Executors

class ConnectionManager(context: Context) {

    interface Listener {
        fun onConnectionChange(isConnected: Boolean)
    }

    private val executorService = Executors.newCachedThreadPool()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var listener: Listener? = null
    var isConnected = networkIsConnected()

    init {
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                executorService.execute {
                    onUpdateStatus()
                }
            }
        }, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    private fun onUpdateStatus() {
        val isConnectedUpdate = networkIsConnected()

        if (isConnected != isConnectedUpdate) {
            isConnected = isConnectedUpdate
            listener?.onConnectionChange(isConnected)
        }
    }

    private fun networkIsConnected(): Boolean {
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }
}
