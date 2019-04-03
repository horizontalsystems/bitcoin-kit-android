package io.horizontalsystems.bitcoinkit.dash.masternodelist

import io.horizontalsystems.bitcoinkit.dash.IHasher
import io.horizontalsystems.bitcoinkit.dash.models.Masternode
import io.horizontalsystems.bitcoinkit.dash.models.MasternodeSerializer

class MasternodeListMerkleRootCalculator(private val masternodeSerializer: MasternodeSerializer, val masternodeHasher: IHasher, val masternodeMerkleRootCreator: MerkleRootCreator) {

    fun calculateMerkleRoot(sortedMasternodes: List<Masternode>): ByteArray? {
        val hashList = mutableListOf<ByteArray>()

        sortedMasternodes.forEach { masternode ->
            val serialized = masternodeSerializer.serialize(masternode)
            hashList.add(masternodeHasher.hash(serialized))
        }

        return masternodeMerkleRootCreator.create(hashList)
    }

}
