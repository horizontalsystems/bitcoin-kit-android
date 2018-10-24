package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.realm.Realm
import io.realm.Sort

class Blockchain(private val network: NetworkParameters) {

    fun connect(merkleBlock: MerkleBlock, realm: Realm): Block {
        val blockInDB = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlock.blockHash)
                .findFirst()

        if (blockInDB != null) {
            return blockInDB
        }

        val parentBlock = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlock.header.prevHash)
                .findFirst() ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        network.validateBlock(block, parentBlock)

        block.stale = true

        return realm.copyToRealm(block)
    }

    fun handleFork(realm: Realm) {
        val firstStaleHeight = realm.where(Block::class.java)
                .equalTo("stale", true)
                .sort("height")
                .findFirst()?.height ?: return

        val lastNotStaleHeight = realm.where(Block::class.java)
                .equalTo("stale", false)
                .sort("height", Sort.DESCENDING)
                .findFirst()?.height ?: 0

        realm.executeTransaction {
            if (firstStaleHeight <= lastNotStaleHeight) {
                val lastStaleHeight = realm.where(Block::class.java)
                        .equalTo("stale", true)
                        .sort("height", Sort.DESCENDING)
                        .findFirst()?.height!!

                if (lastStaleHeight > lastNotStaleHeight) {
                    realm.where(Block::class.java)
                            .equalTo("stale", false)
                            .greaterThanOrEqualTo("height", firstStaleHeight)
                            .findAll()
                            .deleteAllFromRealm()
                } else {
                    realm.where(Block::class.java)
                            .equalTo("stale", true)
                            .findAll()
                            .deleteAllFromRealm()
                }
            }

            realm.where(Block::class.java)
                    .equalTo("stale", true)
                    .findAll()
                    .forEach { staleBlock ->
                        staleBlock.stale = false
                    }
        }
    }

}
