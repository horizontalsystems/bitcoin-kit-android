package io.horizontalsystems.dashkit.masternodelist

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.MasternodeSerializer

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
