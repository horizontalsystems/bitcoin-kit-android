package bitcoin.wallet.kit.headers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.network.NetworkParameters
import io.realm.Sort

class HeaderHandler(private val realmFactory: RealmFactory, private val network: NetworkParameters) {
    private val validator = BlockValidator()

    @Throws
    fun handle(headers: Array<Header>) {
        val realm = realmFactory.realm
        val bestBlock = realm.where(Block::class.java)
                .isNotNull("previousBlock")
                .sort("height", Sort.DESCENDING)
                .findFirst()

        var previousBlock = bestBlock ?: network.checkpointBlock

        // validate chain
        headers.forEach { header ->
            val block = Block(header, previousBlock)

            validator.validate(block)

            val existingBlock = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
            if (existingBlock == null) {
                realm.executeTransaction {
                    previousBlock = it.copyToRealm(block)
                }
            } else {
                if (existingBlock.header == null) {
                    existingBlock.header = block.header
                }

                previousBlock = block
            }
        }

        realm.close()
    }

}
