package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.*
import io.horizontalsystems.bitcoinkit.models.PublicKey

@Dao
interface PublicKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(publicKey: PublicKey)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(keys: List<PublicKey>)

    @Delete
    fun delete(publicKey: PublicKey)

    @Query("select * from PublicKey")
    fun getAll(): List<PublicKey>

    @Query("SELECT * from PublicKey where scriptHashP2WPKH = :keyHash limit 1")
    fun getByScriptHashWPKH(keyHash: ByteArray): PublicKey?

    @Query("SELECT * from PublicKey where publicKey = :keyHash or publicKeyHash =:keyHash limit 1")
    fun getByKeyOrKeyHash(keyHash: ByteArray): PublicKey?

    @Query("select * from PublicKey where path = :path limit 1")
    fun getByPath(path: String): PublicKey?

}
