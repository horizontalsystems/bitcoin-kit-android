package bitcoin.wallet.kit

import android.support.test.InstrumentationRegistry
import bitcoin.wallet.kit.core.RealmFactory
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import io.realm.Realm
import io.realm.RealmConfiguration
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class RealmFactoryMock {
    private val context = InstrumentationRegistry.getTargetContext()

    // Mocks
    @Mock lateinit var realmFactory: RealmFactory

    init {
        MockitoAnnotations.initMocks(this)

        Realm.init(context)

        val configuration = RealmConfiguration.Builder()
                .name("test-db")
                .modules(WalletKitModule())
                .build()

        doAnswer { Realm.getInstance(configuration) }.whenever(realmFactory).realm
    }
}
