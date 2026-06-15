package io.horizontalsystems.bitcoincore.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.horizontalsystems.bitcoincore.core.IConnectionManager
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener

class ConnectionManager(context: Context) : IConnectionManager {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override var listener: IConnectionManagerListener? = null
    override var isConnected: Boolean = false

    private var hasValidInternet = false
    private var hasConnection = false
    private var callback = ConnectionStatusCallback()

    @Volatile
    private var isRegistered = false

    @Synchronized
    override fun onEnterForeground() {
        setInitialValues()
        // Avoid re-registering an already active callback: it does not increase the
        // app's network-callback quota and keeps a single source of connectivity updates.
        if (isRegistered) return
        try {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            isRegistered = true
        } catch (e: Exception) {
            // registerNetworkCallback may throw TooManyRequestsException once the app-wide
            // limit of network callbacks is exceeded. Don't crash; fall back to the value
            // computed by setInitialValues() above.
            isRegistered = false
        }
    }

    @Synchronized
    override fun onEnterBackground() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //already unregistered
        }
        isRegistered = false
    }

    private fun setInitialValues() {
        hasConnection = false
        hasValidInternet = false
        isConnected = getInitialConnectionStatus()
        listener?.onConnectionChange(isConnected)
    }

    private fun getInitialConnectionStatus(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false

        hasConnection = true
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        hasValidInternet = capabilities?.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false

        return hasValidInternet
    }


    inner class ConnectionStatusCallback : ConnectivityManager.NetworkCallback() {

        private val activeNetworks: MutableList<Network> = mutableListOf()

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworks.removeAll { activeNetwork -> activeNetwork == network }
            hasConnection = activeNetworks.isNotEmpty()
            updatedConnectionState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            hasValidInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            updatedConnectionState()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (activeNetworks.none { activeNetwork -> activeNetwork == network }) {
                activeNetworks.add(network)
            }
            hasConnection = activeNetworks.isNotEmpty()
            updatedConnectionState()
        }
    }

    private fun updatedConnectionState() {
        val oldValue = isConnected
        isConnected = hasConnection && hasValidInternet
        if (oldValue != isConnected) {
            listener?.onConnectionChange(isConnected)
        }
    }
}
