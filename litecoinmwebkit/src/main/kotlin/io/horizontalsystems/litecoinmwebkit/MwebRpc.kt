package io.horizontalsystems.litecoinmwebkit

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import io.grpc.stub.StreamObserver
import io.horizontalsystems.litecoinmwebkit.proto.AddressRequest
import io.horizontalsystems.litecoinmwebkit.proto.BroadcastRequest
import io.horizontalsystems.litecoinmwebkit.proto.CreateRequest
import io.horizontalsystems.litecoinmwebkit.proto.CreateResponse
import io.horizontalsystems.litecoinmwebkit.proto.RpcGrpc
import io.horizontalsystems.litecoinmwebkit.proto.SpentRequest
import io.horizontalsystems.litecoinmwebkit.proto.StatusRequest
import io.horizontalsystems.litecoinmwebkit.proto.StatusResponse
import io.horizontalsystems.litecoinmwebkit.proto.Utxo
import io.horizontalsystems.litecoinmwebkit.proto.UtxosRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** gRPC client of the embedded daemon (proto/mwebd.proto). */
class MwebRpc(port: Int) {

    private val channel: ManagedChannel = OkHttpChannelBuilder
        .forAddress("127.0.0.1", port)
        .usePlaintext()
        .build()

    private val stub = RpcGrpc.newStub(channel)

    suspend fun status(): StatusResponse = unary { observer ->
        stub.status(StatusRequest.getDefaultInstance(), observer)
    }

    suspend fun addresses(scanSecret: ByteArray, spendPubKey: ByteArray, from: Int, to: Int): List<String> {
        val request = AddressRequest.newBuilder()
            .setScanSecret(ByteString.copyFrom(scanSecret))
            .setSpendPubkey(ByteString.copyFrom(spendPubKey))
            .setFromIndex(from)
            .setToIndex(to)
            .build()
        return unary<io.horizontalsystems.litecoinmwebkit.proto.AddressResponse> { observer ->
            stub.addresses(request, observer)
        }.addressList
    }

    /**
     * Streams the account's outputs: first those already confirmed from
     * [fromHeight] on, then an empty marker, then unconfirmed and newly
     * confirmed ones as they appear. The flow ends with an error when the
     * daemon stops.
     */
    fun utxos(scanSecret: ByteArray, fromHeight: Int): Flow<Utxo> = callbackFlow {
        val request = UtxosRequest.newBuilder()
            .setScanSecret(ByteString.copyFrom(scanSecret))
            .setFromHeight(fromHeight)
            .build()
        var call: ClientCallStreamObserver<UtxosRequest>? = null
        stub.utxos(request, object : ClientResponseObserver<UtxosRequest, Utxo> {
            override fun beforeStart(requestStream: ClientCallStreamObserver<UtxosRequest>) {
                call = requestStream
            }

            override fun onNext(value: Utxo) {
                trySend(value)
            }

            override fun onError(t: Throwable) {
                close(t)
            }

            override fun onCompleted() {
                close()
            }
        })
        awaitClose { call?.cancel("closed", null) }
    }

    /** Returns the output IDs among [outputIds] that are not in the unspent set. */
    suspend fun spent(outputIds: List<String>): List<String> {
        if (outputIds.isEmpty()) return emptyList()
        val request = SpentRequest.newBuilder().addAllOutputId(outputIds).build()
        return unary<io.horizontalsystems.litecoinmwebkit.proto.SpentResponse> { observer ->
            stub.spent(request, observer)
        }.outputIdList
    }

    suspend fun create(
        rawTx: ByteArray,
        scanSecret: ByteArray,
        spendSecret: ByteArray,
        feeRatePerKb: Long,
        dryRun: Boolean
    ): CreateResponse {
        val request = CreateRequest.newBuilder()
            .setRawTx(ByteString.copyFrom(rawTx))
            .setScanSecret(ByteString.copyFrom(scanSecret))
            .setSpendSecret(ByteString.copyFrom(spendSecret))
            .setFeeRatePerKb(feeRatePerKb)
            .setDryRun(dryRun)
            .build()
        return unary { observer -> stub.create(request, observer) }
    }

    /** Broadcasts a transaction, with its MWEB extension, and returns the txid. */
    suspend fun broadcast(rawTx: ByteArray): String {
        val request = BroadcastRequest.newBuilder().setRawTx(ByteString.copyFrom(rawTx)).build()
        return unary<io.horizontalsystems.litecoinmwebkit.proto.BroadcastResponse> { observer ->
            stub.broadcast(request, observer)
        }.txid
    }

    fun shutdown() {
        channel.shutdownNow()
        try {
            channel.awaitTermination(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private suspend fun <T> unary(call: (StreamObserver<T>) -> Unit): T =
        suspendCancellableCoroutine { continuation ->
            call(object : StreamObserver<T> {
                private var result: T? = null

                override fun onNext(value: T) {
                    result = value
                }

                override fun onError(t: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(t)
                }

                override fun onCompleted() {
                    val value = result
                    if (!continuation.isActive) return
                    if (value == null) {
                        continuation.resumeWithException(IllegalStateException("empty response"))
                    } else {
                        continuation.resume(value)
                    }
                }
            })
        }
}
