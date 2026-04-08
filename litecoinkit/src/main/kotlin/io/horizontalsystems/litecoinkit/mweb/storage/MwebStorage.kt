package io.horizontalsystems.litecoinkit.mweb.storage

import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebOutput
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebSyncState
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebWalletOutput

class MwebStorage(private val db: MwebDatabase) {

    fun saveOutputs(outputs: List<MwebOutput>) = db.mwebDao.insertOutputs(outputs)

    fun saveWalletOutput(output: MwebWalletOutput) = db.mwebDao.insertWalletOutput(output)

    fun saveWalletOutputs(outputs: List<MwebWalletOutput>) = db.mwebDao.insertWalletOutputs(outputs)

    fun totalBalance(): Long = db.mwebDao.getTotalBalance()

    fun unspentOutputCount(): Int = db.mwebDao.getUnspentOutputCount()

    fun getUnspentWalletOutputIds(): List<String> = db.mwebDao.getUnspentWalletOutputIds()

    fun getSpendableOutputs(): List<MwebDao.SpendableOutput> = db.mwebDao.getSpendableOutputs()

    fun markOutputsAsSpent(outputIds: List<String>) = db.mwebDao.markOutputsAsSpent(outputIds)

    fun totalStoredOutputs(): Long = db.mwebDao.getOutputCount()

    fun getSyncState(): MwebSyncState? = db.mwebDao.getSyncState()

    fun updateSyncState(lastLeafIndex: Long) {
        val current = getSyncState() ?: MwebSyncState()
        db.mwebDao.upsertSyncState(
            current.copy(
                lastLeafIndex = maxOf(current.lastLeafIndex, lastLeafIndex),
                storedOutputCount = db.mwebDao.getOutputCount()
            )
        )
    }

    fun clear() {
        db.clearAllTables()
    }
}