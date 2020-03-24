package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.utils.Bip69Sorter
import io.horizontalsystems.bitcoincore.utils.ShuffleSorter
import io.horizontalsystems.bitcoincore.utils.StraightSorter

class TransactionDataSorterFactory : ITransactionDataSorterFactory {
    override fun sorter(type: TransactionDataSortType): ITransactionDataSorter {
        return when (type) {
            TransactionDataSortType.None -> StraightSorter()
            TransactionDataSortType.Shuffle -> ShuffleSorter()
            TransactionDataSortType.Bip69 -> Bip69Sorter()
        }
    }
}
