package io.horizontalsystems.bitcoincore.storage

import androidx.room.*
import io.horizontalsystems.bitcoincore.models.PeerAddress

@Dao
interface PeerAddressDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(peers: List<PeerAddress>)

    @Query("SELECT * FROM PeerAddress WHERE ip NOT IN(:ips) ORDER BY score ASC, connectionTime ASC LIMIT 1")
    fun getLeastScoreFastest(ips: List<String>): PeerAddress?

    @Query("UPDATE PeerAddress SET score = score + 1 WHERE ip = :ip")
    fun increaseScore(ip: String)

    @Query("UPDATE PeerAddress SET connectionTime = :time WHERE ip = :ip")
    fun setConnectionTime(time: Long, ip: String)

    @Delete
    fun delete(peerAddress: PeerAddress)
}
