package io.horizontalsystems.dashkit

interface InstantLockListener {
    fun onUpdateInstantLock(transactionHash: ByteArray)
}
