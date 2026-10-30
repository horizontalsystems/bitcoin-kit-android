package io.horizontalsystems.litecoinmwebkit

import android.content.Context
import android.util.Log
import io.horizontalsystems.litecoinkit.LitecoinKit
import java.io.File
import java.io.FileNotFoundException

/**
 * The one MWEB daemon of the process. It keeps the Litecoin header chain and
 * the MWEB UTXO set in a data directory shared by every wallet on the device
 * (`<filesDir>/mweb/<network>`), so a second wallet or a restored seed does
 * not sync the chain again. Wallets stream their own outputs from it over
 * gRPC by scan secret.
 *
 * The native library hosts a single daemon, so starting it for a different
 * network or proxy stops the running one first.
 */
object MwebDaemon {
    private const val TAG = "MwebDaemon"

    data class Config(val networkType: LitecoinKit.NetworkType, val proxyUrl: String?)

    private var running: Config? = null
    private var port = 0

    val isRunning: Boolean
        @Synchronized get() = running != null

    /** Starts the daemon (or returns the running one) and returns its gRPC port on 127.0.0.1. */
    @Synchronized
    @Throws(MwebNativeException::class)
    fun start(context: Context, networkType: LitecoinKit.NetworkType, proxyUrl: String?): Int {
        val config = Config(networkType, proxyUrl?.takeIf { it.isNotBlank() })
        if (running == config) return port
        if (running != null) stop()

        val dataDir = dataDir(context, networkType).apply { mkdirs() }
        val checkpoint = checkpointFile(context, networkType)

        Log.i(TAG, "starting ${chainName(networkType)} daemon in $dataDir" +
                (config.proxyUrl?.let { " via $it" } ?: "") +
                (checkpoint?.let { ", checkpoint ${it.name}" } ?: ""))
        port = MwebNative.start(
            dataDir.absolutePath,
            chainName(networkType),
            config.proxyUrl,
            checkpoint?.absolutePath,
            true
        )
        running = config
        return port
    }

    @Synchronized
    fun stop() {
        if (running == null) return
        Log.i(TAG, "stopping daemon")
        MwebNative.stop()
        running = null
        port = 0
    }

    /** Stops the daemon and restarts it with the same configuration, e.g. after a connectivity change. */
    @Synchronized
    @Throws(MwebNativeException::class)
    fun restart(context: Context): Int {
        val config = running ?: return 0
        stop()
        return start(context, config.networkType, config.proxyUrl)
    }

    fun dataDir(context: Context, networkType: LitecoinKit.NetworkType): File =
        File(File(context.filesDir, "mweb"), chainName(networkType))

    /** Deletes the shared chain data of [networkType]; the daemon must not be running. */
    @Synchronized
    fun deleteData(context: Context, networkType: LitecoinKit.NetworkType) {
        check(running?.networkType != networkType) { "daemon is running" }
        dataDir(context, networkType).deleteRecursively()
    }

    fun chainName(networkType: LitecoinKit.NetworkType) = when (networkType) {
        LitecoinKit.NetworkType.MainNet -> "mainnet"
        LitecoinKit.NetworkType.TestNet -> "testnet"
    }

    /**
     * Copies the bundled header checkpoint for [networkType] out of the
     * assets so the native side can read it. The daemon only uses it for an
     * empty data directory. Returns null when the kit ships no bundle for
     * the network.
     */
    private fun checkpointFile(context: Context, networkType: LitecoinKit.NetworkType): File? {
        val name = "checkpoint-${chainName(networkType)}.bin"
        val target = File(context.cacheDir, "mweb-$name")
        return try {
            context.assets.open("mweb/$name").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        } catch (e: FileNotFoundException) {
            null
        }
    }
}
