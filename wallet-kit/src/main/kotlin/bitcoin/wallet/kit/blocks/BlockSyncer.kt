package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.PeerGroup
import kotlin.concurrent.thread

class BlockSyncer(private val realmFactory: RealmFactory, val peerGroup: PeerGroup) {

    fun enqueueRun() {
        thread {
            run()
        }
    }

    private fun run() {
        val realm = realmFactory.realm

        val nonSyncedBlocks = realm.where(Block::class.java).equalTo("synced", false).findAll()
        val hashes = nonSyncedBlocks.map { it.headerHash }

        realm.close()

        if (hashes.isNotEmpty()) {
            peerGroup.requestMerkleBlocks(hashes.toTypedArray())
        }
    }
}
