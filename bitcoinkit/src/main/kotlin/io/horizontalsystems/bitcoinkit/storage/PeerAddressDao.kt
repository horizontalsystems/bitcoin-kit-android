package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bitcoinkit.models.PeerAddress

@Dao
interface PeerAddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(peers: List<PeerAddress>)

    @Query("SELECT * FROM PeerAddress WHERE ip IN(:ips)")
    fun getExisting(ips: List<String>): List<PeerAddress>

    @Query("SELECT * FROM PeerAddress WHERE ip NOT IN(:ips) ORDER BY ip DESC LIMIT 1")
    fun getLeastScore(ips: List<String>): PeerAddress?

    @Query("UPDATE PeerAddress SET score = score + 1 WHERE ip = :ip")
    fun increaseScore(ip: String)

    @Query("DELETE FROM PeerAddress where ip = :ip")
    fun delete(ip: String)
}
