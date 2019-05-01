package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import io.horizontalsystems.bitcoincore.models.*

@Database(version = 1, exportSchema = false, entities = [
    FeeRate::class,
    BlockchainState::class,
    PeerAddress::class,
    BlockHash::class,
    Block::class,
    SentTransaction::class,
    Transaction::class,
    TransactionInput::class,
    TransactionOutput::class,
    PublicKey::class
])

abstract class CoreDatabase : RoomDatabase() {

    abstract val feeRate: FeeRateDao
    abstract val blockchainState: BlockchainStateDao
    abstract val peerAddress: PeerAddressDao
    abstract val blockHash: BlockHashDao
    abstract val block: BlockDao
    abstract val sentTransaction: SentTransactionDao
    abstract val transaction: TransactionDao
    abstract val input: TransactionInputDao
    abstract val output: TransactionOutputDao
    abstract val publicKey: PublicKeyDao

}
