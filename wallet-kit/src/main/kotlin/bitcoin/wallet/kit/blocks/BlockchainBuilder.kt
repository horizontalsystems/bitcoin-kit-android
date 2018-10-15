package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.blocks.validators.BlockValidatorException
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.network.NetworkParameters
import io.realm.Realm

class BlockchainBuilder(private val network: NetworkParameters) {

    fun buildChain(merkleBlocks: List<MerkleBlock>, realm: Realm): Map<String, Block> {
        val merkleBlockFirst = merkleBlocks.first()

        var parentBlock = realm.where(Block::class.java)
                .equalTo("headerHash", merkleBlockFirst.header.prevHash)
                .findFirst() ?: throw BlockValidatorException.NoPreviousBlock()

        val blocks = mutableMapOf<String, Block>()

        merkleBlocks.forEach { merkleBlock ->
            val block = Block(merkleBlock.header, parentBlock)
            network.validateBlock(block, parentBlock)
            blocks[merkleBlock.reversedHeaderHashHex] = block

            parentBlock = block
        }

        return blocks
    }

}
