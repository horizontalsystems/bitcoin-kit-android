package io.horizontalsystems.tools.peer

interface IPeerTaskHandler {
    fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean
}

class PeerTaskHandlerChain : IPeerTaskHandler {

    private val concreteHandlers = mutableListOf<IPeerTaskHandler>()

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return concreteHandlers.any {
            it.handleCompletedTask(peer, task)
        }
    }

    fun addHandler(h: IPeerTaskHandler) {
        concreteHandlers.add(h)
    }

}
