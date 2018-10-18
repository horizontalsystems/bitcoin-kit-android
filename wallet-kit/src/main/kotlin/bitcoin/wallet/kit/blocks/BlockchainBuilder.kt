package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.blocks.validators.BlockValidatorException
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.network.NetworkParameters
import io.realm.Realm

class BlockchainBuilder(private val network: NetworkParameters) {

    fun connect(merkleBlock: MerkleBlock, realm: Realm): Block {
        val parentBlock = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlock.header.prevHash)
                .findFirst() ?: throw BlockValidatorException.NoPreviousBlock()

        val block = Block(merkleBlock.header, parentBlock)
        network.validateBlock(block, parentBlock)

        return block
    }

}
