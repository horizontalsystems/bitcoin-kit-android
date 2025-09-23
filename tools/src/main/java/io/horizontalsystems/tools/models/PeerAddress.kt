package io.horizontalsystems.tools.models


data class PeerAddress(var ip: String, var score: Int = 0, var connectionTime: Long? = null)
