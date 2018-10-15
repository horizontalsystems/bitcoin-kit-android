package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.PublicKey
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.BlockHash
import bitcoin.wallet.kit.network.PeerGroup
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class InitialSyncer(
        private val realmFactory: RealmFactory,
        private val blockDiscover: BlockDiscover,
        private val stateManager: StateManager,
        private val addressManager: AddressManager,
        private val peerGroup: PeerGroup,
        private val scheduler: Scheduler = Schedulers.io()) {

    private val disposables = CompositeDisposable()

    @Throws
    fun sync() {
        if (!stateManager.apiSynced) {
            val externalObservable = blockDiscover.fetchFromApi(true)
            val internalObservable = blockDiscover.fetchFromApi(false)

            val disposable = Observable
                    .zip(externalObservable, internalObservable, BiFunction<Pair<List<PublicKey>, List<BlockHash>>, Pair<List<PublicKey>, List<BlockHash>>, Pair<List<PublicKey>, List<BlockHash>>> { external, internal ->
                        val (externalKeys, externalBlocks) = external
                        val (internalKeys, internalBlocks) = internal

                        Pair(externalKeys + internalKeys, externalBlocks + internalBlocks)
                    })
                    .subscribeOn(scheduler)
                    .subscribe(
                            { (keys, blockHashes) ->
                                handle(keys, blockHashes)
                            },
                            {
                                print("Initial Sync Error: $it")
                            }
                    )

            disposables.add(disposable)
        } else {
            peerGroup.start()
        }
    }

    @Throws
    private fun handle(keys: List<PublicKey>, blockHashes: List<BlockHash>) {

        val realm = realmFactory.realm

        realm.executeTransaction {
            it.insertOrUpdate(blockHashes)
        }

        addressManager.addKeys(keys)

        realm.close()

        stateManager.apiSynced = true
        peerGroup.start()
    }

}
