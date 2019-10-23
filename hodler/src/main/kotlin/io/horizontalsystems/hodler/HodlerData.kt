package io.horizontalsystems.hodler

import io.horizontalsystems.bitcoincore.core.IPluginData

data class HodlerData(val lockTimeInterval: LockTimeInterval) : IPluginData
