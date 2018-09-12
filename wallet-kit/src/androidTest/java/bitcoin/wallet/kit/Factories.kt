package bitcoin.wallet.kit

import android.support.test.InstrumentationRegistry
import bitcoin.wallet.kit.TestUtils.whenever
import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.transactions.TransactionLinker
import io.realm.Realm
import io.realm.RealmConfiguration
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class Factories {
    private val context = InstrumentationRegistry.getTargetContext()

    // Mocks
    @Mock lateinit var realmFactory: RealmFactory
    @Mock lateinit var transactionLinker: TransactionLinker

    init {
        MockitoAnnotations.initMocks(this)

        Realm.init(context)

        val configuration = RealmConfiguration.Builder()
                .inMemory()
                .modules(WalletKitModule())
                .build()

        whenever(realmFactory.realm).thenReturn(Realm.getInstance(configuration))
    }
}
