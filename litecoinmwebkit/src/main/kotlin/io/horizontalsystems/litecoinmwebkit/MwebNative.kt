package io.horizontalsystems.litecoinmwebkit

/**
 * JNI bridge to libmwebkit.so, which wraps the MWEB daemon library
 * (external-libs/include/mwebd.h). One daemon instance exists per process.
 */
internal object MwebNative {

    init {
        System.loadLibrary("mwebkit")
    }

    /**
     * Starts the daemon and returns the loopback gRPC port. [chain] is
     * "mainnet", "testnet" or "regtest"; [proxyUrl] a SOCKS URL such as
     * "socks5://127.0.0.1:9050" or null; [checkpointPath] a header checkpoint
     * bundle or null to sync from genesis.
     */
    @Throws(MwebNativeException::class)
    external fun start(
        dataDir: String,
        chain: String,
        proxyUrl: String?,
        checkpointPath: String?,
        blocksOnly: Boolean
    ): Int

    external fun stop()

    /**
     * Derives the MWEB addresses with indices in [from, to) for the account
     * given by its hex scan secret and hex compressed spend public key,
     * newline separated.
     */
    @Throws(MwebNativeException::class)
    external fun addresses(
        scanSecretHex: String,
        spendPubKeyHex: String,
        chain: String,
        from: Int,
        to: Int
    ): String
}

class MwebNativeException(message: String) : Exception(message)
