package io.horizontalsystems.tools

import io.horizontalsystems.tools.peer.Peer


interface IConnectionManager {
    val listener: IConnectionManagerListener?
    val isConnected: Boolean
    fun onEnterForeground()
    fun onEnterBackground()
}

interface IConnectionManagerListener {
    fun onConnectionChange(isConnected: Boolean)
}

interface IPeerAddressManager {
    val listener: IPeerAddressManagerListener?
    val hasFreshIps: Boolean
    fun getIp(): String?
    fun addIps(ips: List<String>)
    fun markFailed(ip: String)
    fun markSuccess(ip: String)
    fun markConnected(peer: Peer)
}

interface IPeerAddressManagerListener {
    fun onAddAddress()
}
