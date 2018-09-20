package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.PeerGroup

class HeaderSyncer(private val realmFactory: RealmFactory, private val peerGroup: PeerGroup, val network: NetworkParameters) {

    fun sync() {
        val realm = realmFactory.realm
        val blocksInChain = realm.where(Block::class.java)
                .isNotNull("previousBlock")
                .sort("height")

        val blocks = mutableListOf<Block>()
        val lastBlockInDatabase = blocksInChain.findAll().lastOrNull()
        if (lastBlockInDatabase != null) {
            blocks.add(lastBlockInDatabase)

            val thresholdBlock = blocksInChain.equalTo("height", lastBlockInDatabase.height - 100).findFirst()
            if (thresholdBlock != null) {
                blocks.add(thresholdBlock)
            } else {
                blocksInChain
                        .lessThanOrEqualTo("height", lastBlockInDatabase.height)
                        .findFirst()
                        // pick up checkpoint block
                        ?.previousBlock
                        ?.let { checkpointBlock -> blocks.add(checkpointBlock) }
            }

        } else {
            blocks.add(network.checkpointBlock)
        }

        peerGroup.requestHeaders(blocks.map { it.headerHash }.toTypedArray())

        realm.close()
    }
}
