package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.util.Log
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RoomDatabaseTest {
    private lateinit var db: KitDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getTargetContext()
        db = Room.inMemoryDatabaseBuilder(context, KitDatabase::class.java).build()

        var height = 100
        Observable
                .interval(1000, TimeUnit.MILLISECONDS)
                .subscribe {
                    height += 1

                    if (height < 1000) {
                        Log.e("AAA", "inserting in background: $height")
                        db.peerAddress.insertAll(listOf(PeerAddress("$height")))
                    }

                }
    }

    @After
    fun after() {
        db.close()
    }

    @Test
    fun transactions() {
        Thread.sleep(1000)

        try {
            db.runInTransaction {
                for (i in 0..10) {
                    Thread.sleep(1000)
                    Log.e("AAA", ": inserting in forground: $i")
                    db.blockHash.insert(BlockHash("$i", i))
                }

                Log.e("AAA", "inserting in forground: 11")
                // db.blockHash.insert(BlockHash("11", "2000000000000000".toInt()))
            }
        } catch (e: Exception) {
            println(e.message)
        }

        Thread.sleep(1000)
        println("")
    }
}
