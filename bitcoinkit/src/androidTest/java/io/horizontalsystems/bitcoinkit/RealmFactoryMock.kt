package io.horizontalsystems.bitcoinkit

import android.support.test.InstrumentationRegistry
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.RealmFactory
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
                .modules(BitcoinKitModule())
                .build()

        doAnswer { Realm.getInstance(configuration) }.whenever(realmFactory).realm
    }
}
