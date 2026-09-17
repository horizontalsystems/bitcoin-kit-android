package io.horizontalsystems.dogecoinkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairApi
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairBlockHashFetcher
import io.horizontalsystems.bitcoincore.apisync.blockchair.BlockchairTransactionProvider
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.core.DoubleSha256Hasher
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager
import io.horizontalsystems.bitcoincore.managers.Bip44RestoreKeyConverter
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.Checkpoint
import io.horizontalsystems.bitcoincore.models.WatchAddressPublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDWallet.Purpose
import io.horizontalsystems.hdwalletkit.Mnemonic

/**
 * Dogecoin is a BIP44-only, Base58-only UTXO chain: no segwit, no RBF, no derivation choice.
 * That makes this kit closer to DashKit than to LitecoinKit, minus the masternode machinery.
 *
 * The one thing that is not like its siblings is merge mining — see [AuxPoW].
 */
class DogecoinKit : AbstractKit {
    enum class NetworkType {
        MainNet, TestNet
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    constructor(
        context: Context,
        words: List<String>,
        passphrase: String,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) : this(context, Mnemonic().toSeed(words, passphrase), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    constructor(
        context: Context,
        seed: ByteArray,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) : this(context, HDExtendedKey(seed, purpose), walletId, networkType, peerSize, syncMode, confirmationsThreshold)

    /**
     * @constructor Creates and initializes the DogecoinKit
     * @param context The Android context
     * @param extendedKey HDExtendedKey that contains HDKey and version
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Blockchair().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     */
    constructor(
        context: Context,
        extendedKey: HDExtendedKey,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) {
        requireSupportedSyncMode(syncMode)
        network = network(networkType)

        bitcoinCore = bitcoinCore(
            context = context,
            extendedKey = extendedKey,
            watchAddressPublicKey = null,
            networkType = networkType,
            walletId = walletId,
            syncMode = syncMode,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )
    }

    /**
     * @constructor Creates and initializes the DogecoinKit
     * @param context The Android context
     * @param watchAddress address for watching in read-only mode
     * @param walletId an arbitrary ID of type String.
     * @param networkType The network type. The default is MainNet.
     * @param peerSize The # of peer-nodes required. The default is 10 peers.
     * @param syncMode How the kit syncs with the blockchain. The default is SyncMode.Blockchair().
     * @param confirmationsThreshold How many confirmations required to be considered confirmed. The default is 6 confirmations.
     */
    constructor(
        context: Context,
        watchAddress: String,
        walletId: String,
        networkType: NetworkType = defaultNetworkType,
        peerSize: Int = defaultPeerSize,
        syncMode: SyncMode = defaultSyncMode,
        confirmationsThreshold: Int = defaultConfirmationsThreshold
    ) {
        requireSupportedSyncMode(syncMode)
        network = network(networkType)

        val address = parseAddress(watchAddress, network)
        val watchAddressPublicKey = WatchAddressPublicKey(address.lockingScriptPayload, address.scriptType)

        bitcoinCore = bitcoinCore(
            context = context,
            extendedKey = null,
            watchAddressPublicKey = watchAddressPublicKey,
            networkType = networkType,
            walletId = walletId,
            syncMode = syncMode,
            peerSize = peerSize,
            confirmationsThreshold = confirmationsThreshold
        )
    }

    private fun bitcoinCore(
        context: Context,
        extendedKey: HDExtendedKey?,
        watchAddressPublicKey: WatchAddressPublicKey?,
        networkType: NetworkType,
        walletId: String,
        syncMode: SyncMode,
        peerSize: Int,
        confirmationsThreshold: Int
    ): BitcoinCore {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode))
        val storage = Storage(database)
        val checkpoint = Checkpoint.resolveCheckpoint(syncMode, network, storage)
        val apiSyncStateManager = ApiSyncStateManager(storage, network.syncableFromApi && syncMode !is SyncMode.Full)
        val blockchairApi = BlockchairApi(network.blockchairChainId)
        val apiTransactionProvider = apiTransactionProvider(networkType, blockchairApi)
        val paymentAddressParser = PaymentAddressParser("dogecoin", removeScheme = true)

        val bitcoinCore = BitcoinCoreBuilder()
            .setContext(context)
            .setExtendedKey(extendedKey)
            .setWatchAddressPublicKey(watchAddressPublicKey)
            .setPurpose(purpose)
            .setNetwork(network)
            .setCheckpoint(checkpoint)
            .setPaymentAddressParser(paymentAddressParser)
            .setPeerSize(peerSize)
            .setSyncMode(syncMode)
            .setSendType(BitcoinCore.SendType.API(blockchairApi))
            .setConfirmationThreshold(confirmationsThreshold)
            .setStorage(storage)
            .setApiTransactionProvider(apiTransactionProvider)
            .setApiSyncStateManager(apiSyncStateManager)
            .setBlockValidator(blockValidatorSet())
            .build()

        //  extending bitcoinCore

        // Core's parsers read a fixed 80 byte header and would land on the AuxPoW blob instead of
        // the transaction count. They are keyed by command in a map and registered first, so
        // adding ours here replaces them. Only `merkleblock` is reachable while Blockchair is the
        // one supported sync mode; `headers` is registered so the pair cannot drift apart.
        val blockHeaderParser = DogecoinBlockHeaderParser(DoubleSha256Hasher())
        bitcoinCore.addMessageParser(DogecoinMerkleBlockMessageParser(blockHeaderParser))
            .addMessageParser(DogecoinHeadersMessageParser(blockHeaderParser))

        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)
        bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))

        return bitcoinCore
    }

    private fun parseAddress(address: String, network: Network): Address {
        val addressConverter = AddressConverterChain().apply {
            prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
        }
        return addressConverter.convert(address)
    }

    /**
     * Deliberately empty, and the reason [requireSupportedSyncMode] exists.
     *
     * Dogecoin's proof of work is AuxPoW: for a merge-mined block the scrypt hash that must meet
     * the target is the hash of the *Litecoin* parent header, and proving it commits to this block
     * means walking two merkle branches and scanning the parent coinbase. Every block on the chain
     * today is merge-mined, so a validator that skipped AuxPoW blocks would validate nothing at all
     * while looking like it validated everything.
     *
     * Blockchair sync does not need one: bitcoincore takes the `forceAdd` path, which never calls a
     * validator, and a peer still cannot lie to us there — it can only answer with a block whose
     * header hashes to the hash the API named, and MerkleBlockExtractor binds the transactions to
     * that header's merkle root. What is trusted is the API, for heights and for the chain tip.
     * That is the same model Bitcoin, Bitcoin Cash and Litecoin already run under in this app.
     *
     * If full SPV is wanted later, this is where DigiShield and an AuxPoW proof-of-work validator
     * go, and libdohj's AuxPoWTest is a ready-made specification for the latter.
     */
    private fun blockValidatorSet() = BlockValidatorSet()

    private fun apiTransactionProvider(
        networkType: NetworkType,
        blockchairApi: BlockchairApi
    ) = when (networkType) {
        NetworkType.MainNet -> {
            val blockchairBlockHashFetcher = BlockchairBlockHashFetcher(blockchairApi)
            BlockchairTransactionProvider(blockchairApi, blockchairBlockHashFetcher)
        }

        NetworkType.TestNet -> TODO()
    }

    /**
     * Thrown for any sync mode other than [SyncMode.Blockchair].
     *
     * The other two modes download a header chain from peers, and since this kit installs no block
     * validators (see [blockValidatorSet]) it would accept that chain unverified: a peer could
     * serve a fabricated chain with arbitrary heights, and nothing would notice. Blockchair mode
     * never asks for headers at all.
     *
     * Supporting Api or Full means implementing AuxPoW proof-of-work verification and DigiShield
     * first. Until then, failing loudly beats syncing quietly against an unverified chain.
     */
    class UnsupportedSyncMode(syncMode: SyncMode) : IllegalArgumentException(
        "DogecoinKit supports SyncMode.Blockchair only, got SyncMode.${syncMode.javaClass.simpleName}"
    )

    companion object {
        val purpose: Purpose = Purpose.BIP44

        val defaultNetworkType: NetworkType = NetworkType.MainNet
        val defaultSyncMode: SyncMode = SyncMode.Blockchair()

        internal fun requireSupportedSyncMode(syncMode: SyncMode) {
            if (syncMode !is SyncMode.Blockchair) {
                throw UnsupportedSyncMode(syncMode)
            }
        }
        const val defaultPeerSize: Int = 10
        const val defaultConfirmationsThreshold: Int = 6

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode): String =
            "Dogecoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.Blockchair())) {
                try {
                    SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode)))
                } catch (ex: Exception) {
                    continue
                }
            }
        }

        private fun network(networkType: NetworkType) = when (networkType) {
            NetworkType.MainNet -> MainNetDogecoin()
            NetworkType.TestNet -> TODO()
        }

        private fun addressConverter(network: Network): AddressConverterChain {
            return AddressConverterChain().apply {
                prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
            }
        }

        fun firstAddress(
            seed: ByteArray,
            networkType: NetworkType = NetworkType.MainNet,
        ): Address {
            return BitcoinCore.firstAddress(
                seed,
                purpose,
                network(networkType),
                addressConverter(network(networkType))
            )
        }

        fun firstAddress(
            extendedKey: HDExtendedKey,
            networkType: NetworkType = NetworkType.MainNet,
        ): Address {
            return BitcoinCore.firstAddress(
                extendedKey,
                purpose,
                network(networkType),
                addressConverter(network(networkType))
            )
        }
    }
}
