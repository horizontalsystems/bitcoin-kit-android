package io.horizontalsystems.bitcoincore.network.peer

import io.horizontalsystems.bitcoincore.models.InventoryItem

interface IInventoryItemsHandler {
    fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>)
}

class InventoryItemsHandlerChain : IInventoryItemsHandler {

    private val concreteHandlers = mutableListOf<IInventoryItemsHandler>()

    override fun handleInventoryItems(peer: Peer, inventoryItems: List<InventoryItem>) {
        concreteHandlers.forEach {
            it.handleInventoryItems(peer, inventoryItems)
        }
    }

    fun addHandler(h: IInventoryItemsHandler) {
        concreteHandlers.add(h)
    }

}
