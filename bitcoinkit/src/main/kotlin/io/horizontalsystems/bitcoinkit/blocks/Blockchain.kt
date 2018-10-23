package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.MerkleBlock
import io.horizontalsystems.bitcoinkit.network.NetworkParameters
import io.realm.Realm

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

        return realm.copyToRealm(block)
    }

}
