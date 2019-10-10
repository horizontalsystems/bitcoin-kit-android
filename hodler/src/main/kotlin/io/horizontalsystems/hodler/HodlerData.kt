package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.InvalidPluginDataException

class HodlerData(val lockedUntilTimestamp: Long, val addressString: String) {

    override fun toString(): String {
        return listOf(lockedUntilTimestamp.toString(), addressString).joinToString("|")
    }

    companion object {
        fun parse(serialized: String?): HodlerData {
            val (lockedUntilTimestamp, addressString) = serialized?.split("|") ?: throw InvalidPluginDataException()

            return HodlerData(lockedUntilTimestamp.toLong(), addressString)
        }
    }
}
