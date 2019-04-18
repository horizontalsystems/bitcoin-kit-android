package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.crypto.BloomFilter
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup.IPeerGroupListener
import java.util.concurrent.CopyOnWriteArrayList

class BloomFilterLoader(private val bloomFilterManager: BloomFilterManager) : IPeerGroupListener, BloomFilterManager.Listener {
    private val peers = CopyOnWriteArrayList<Peer>()

    override fun onPeerConnect(peer: Peer) {
        bloomFilterManager.bloomFilter?.let {
            peer.filterLoad(it)
        }
        peers.add(peer)
    }

    override fun onPeerDisconnect(peer: Peer, e: Exception?) {
        peers.remove(peer)
    }

    override fun onFilterUpdated(bloomFilter: BloomFilter) {
        peers.forEach {
            it.filterLoad(bloomFilter)
        }
    }
}
