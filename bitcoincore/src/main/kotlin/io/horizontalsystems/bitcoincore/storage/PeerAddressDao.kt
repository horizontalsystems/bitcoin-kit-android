package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.PeerAddress

@Dao
interface PeerAddressDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(peers: List<PeerAddress>)

    @Query("SELECT * FROM PeerAddress WHERE ip NOT IN(:ips) ORDER BY ip DESC LIMIT 1")
    fun getLeastScore(ips: List<String>): PeerAddress?

    @Query("UPDATE PeerAddress SET score = score + 1 WHERE ip = :ip")
    fun increaseScore(ip: String)

    @Delete
    fun delete(peerAddress: PeerAddress)
}
