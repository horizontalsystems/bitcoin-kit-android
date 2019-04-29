package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoincore.models.PublicKey

@Dao
interface PublicKeyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(keys: List<PublicKey>)

    @Delete
    fun delete(publicKey: PublicKey)

    @Query("select * from PublicKey")
    fun getAll(): List<PublicKey>

    @Query("SELECT * from PublicKey where scriptHashP2WPKH = :keyHash limit 1")
    fun getByScriptHashWPKH(keyHash: ByteArray): PublicKey?

    @Query("SELECT * from PublicKey where publicKey = :keyHash or publicKeyHash =:keyHash limit 1")
    fun getByKeyOrKeyHash(keyHash: ByteArray): PublicKey?

}
