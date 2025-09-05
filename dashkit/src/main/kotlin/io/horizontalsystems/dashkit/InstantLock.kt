package io.horizontalsystems.dashkit

import android.util.Log
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.dashkit.instantsend.InstantTransactionManager
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class InstantLock(
    private val instantTransactionManager: InstantTransactionManager,
) {

    private val disposables = CompositeDisposable()
    private val pollingIntervalSeconds = 10L // Poll every 10 seconds
    private val pollingTimeoutMinutes = 5L   // Timeout after 1 minute
    var instantLockListener: InstantLockListener? = null

    fun handle(transaction: DashTransactionInfo) {
        // If already locked, no need to poll
        if (transaction.txlock) return

        val transactionHashString = transaction.transactionHash
        val txHashBytes = transaction.transactionHash.hexToByteArray().reversedArray()

        Log.e("InstantLock", "Starting to poll for txlock status for tx: $transactionHashString")

        val pollingObservable = Observable
            .interval(0, pollingIntervalSeconds, TimeUnit.SECONDS, Schedulers.io())
            .flatMapSingle { _ ->
                fetchTxLockStatus(transactionHashString)
            }
            .takeUntil(Observable.timer(pollingTimeoutMinutes, TimeUnit.MINUTES, Schedulers.io()))
            .filter { isLocked ->
                isLocked // Only proceed if txlock is true
            }
            .firstElement() // Takes the first true emission, or completes if timeout/no true emission

        disposables.add(
            pollingObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation()) // Or Schedulers.io() if makeTransactionLocked is heavy
                .subscribe(
                    { wasLocked -> // This will only be called if txlock became true
                        if (wasLocked) {
                            Log.e("InstantLock", "txlock confirmed TRUE for $transactionHashString from API. Marking as locked.")
                            instantTransactionManager.makeTransactionLocked(txHashBytes)
                            instantLockListener?.onUpdateInstantLock(txHashBytes)
                        }
                    },
                    { error ->
                        Log.e("InstantLock", "Error polling for txlock status for $transactionHashString: ${error.message}", error)
                    },
                    { // onComplete
                        // This will be called if the observable completes without txlock becoming true
                        // (e.g., timeout occurred and filter didn't pass, or stream completed early for other reasons)
                        // We can check if it was already marked locked by another mechanism or log timeout.
                        // To be more precise about timeout, we'd need to combine with a flag or check if the onSuccess from firstElement was called.
                        // However, firstElement().subscribe's onComplete means it finished, either due to success or upstream completion.
                        // If onSuccess was called, onComplete is also called. If it timed out, filter resulted in empty,
                        // firstElement completes without item, so only onComplete is called.
                        if (!instantTransactionManager.isTransactionLocked(txHashBytes)) {
                            Log.e("InstantLock", "Polling for $transactionHashString completed or timed out without txlock confirmation from API.")
                        }
                    }
                )
        )
    }

    private fun fetchTxLockStatus(transactionHash: String): Single<Boolean> {
        return Single.fromCallable {
            val urlStr = "https://insight.dash.org/insight-api/tx/$transactionHash"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            var isLocked = false
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000 // 15 seconds
                connection.readTimeout = 15000  // 15 seconds
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val responseString = reader.use { it.readText() }
                    reader.close()
                    inputStream.close()

                    val jsonObject = JSONObject(responseString)
                    if (jsonObject.has("txlock")) {
                        isLocked = jsonObject.getBoolean("txlock")
                        Log.e("InstantLock", "API check for $transactionHash: txlock = $isLocked")
                    } else {
                        Log.e("InstantLock", "txlock field not found in API response for $transactionHash")
                        // Optionally, throw an exception or return a specific error state
                    }
                } else {
                    Log.e("InstantLock", "HTTP error code: $responseCode for $transactionHash")
                    // Optionally, throw an exception
                }
            } catch (e: Exception) {
                Log.e("InstantLock", "Exception during fetchTxLockStatus for $transactionHash: ${e.message}")
                throw e // Propagate error to RxJava stream
            } finally {
                connection.disconnect()
            }
            isLocked // Return the status
        }
    }

    // Call this method when InstantLock is no longer needed to clean up disposables
    fun dispose() {
        disposables.clear()
        Log.d("InstantLock", "Disposables cleared")
    }
}
