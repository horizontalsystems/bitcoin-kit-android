package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.Factory
import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.PeerGroup
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.internal.schedulers.IoScheduler

class InitialSyncer {
    private val disposables = CompositeDisposable()

    private val realmFactory: RealmFactory
    private val hdWallet: HDWallet
    private val stateManager: StateManager
    private val apiManager: ApiManager
    private val factory: Factory
    private val peerGroup: PeerGroup
    private val network: NetworkParameters
    private val scheduler: Scheduler

    constructor(realmFactory: RealmFactory, hdWallet: HDWallet, stateManager: StateManager, apiManager: ApiManager, factory: Factory, peerGroup: PeerGroup, network: NetworkParameters, scheduler: Scheduler = IoScheduler()) {
        this.realmFactory = realmFactory
        this.hdWallet = hdWallet
        this.stateManager = stateManager
        this.apiManager = apiManager
        this.factory = factory
        this.peerGroup = peerGroup
        this.network = network
        this.scheduler = scheduler
    }

    @Throws
    fun sync() {
        if (!stateManager.apiSynced) {
            val maxHeight = network.checkpointBlock.height

            val externalObservable = fetchFromApi(true, maxHeight)
            val internalObservable = fetchFromApi(false, maxHeight)

            val disposable = Observable
                    .zip(externalObservable, internalObservable, BiFunction<Pair<List<PublicKey>, List<Block>>, Pair<List<PublicKey>, List<Block>>, Pair<List<PublicKey>, List<Block>>> { external, internal ->
                        val (externalKeys, externalBlocks) = external
                        val (internalKeys, internalBlocks) = internal

                        Pair(externalKeys + internalKeys, externalBlocks + internalBlocks)
                    })
                    .subscribeOn(scheduler)
                    .doOnError {
                        print("Initial Sync Error: $it")
                    }
                    .subscribe { (keys, blocks) ->
                        handle(keys, blocks)
                    }

            disposables.add(disposable)
        } else {
            peerGroup.start()
        }
    }

    @Throws
    private fun handle(keys: List<PublicKey>, blocks: List<Block>) {

        val realm = realmFactory.realm

        realm.executeTransaction {
            it.insertOrUpdate(keys)
            it.insertOrUpdate(blocks)
        }

        stateManager.apiSynced = true
        peerGroup.start()
    }

    @Throws
    private fun fetchFromApi(external: Boolean, maxHeight: Int, lastUsedKeyIndex: Int = -1, keys: List<PublicKey> = listOf(), blocks: List<Block> = listOf()): Observable<Pair<List<PublicKey>, List<Block>>> {
        val count = keys.size
        val gapLimit = hdWallet.gapLimit

        val newKey = hdWallet.publicKey(count, external)

        return apiManager.getBlockHashes(newKey.address)
                .flatMap { blockResponses ->
                    var lastUsedKeyIndexNew = lastUsedKeyIndex

                    if (!blockResponses.isEmpty()) {
                        lastUsedKeyIndexNew = keys.size
                    }

                    val keysNew = keys + listOf(newKey)

                    if (lastUsedKeyIndexNew < keysNew.size - gapLimit) {
                        Observable.just(Pair(keysNew, blocks))
                    } else {
                        val validResponses = blockResponses.filter { it.height < maxHeight }

                        val validBlocks = validResponses.mapNotNull { response ->
                            try {
                                factory.block(response.hash.hexStringToByteArray().reversedArray(), response.height)
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }

                        fetchFromApi(external, maxHeight, lastUsedKeyIndexNew, keysNew, blocks + validBlocks)
                    }
                }
    }

}
