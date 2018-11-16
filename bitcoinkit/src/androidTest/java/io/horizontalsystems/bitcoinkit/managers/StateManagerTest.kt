package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.network.RegTest
import io.realm.Realm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StateManagerTest {

    private val factory = RealmFactoryMock()
    private lateinit var realm: Realm

    private lateinit var stateManager: StateManager

    @Before
    fun setUp() {
        realm = factory.realmFactory.realm
        realm.executeTransaction { it.deleteAll() }

        stateManager = StateManager(factory.realmFactory, MainNet())
    }

    @Test
    fun apiSynced_RegTest() {
        assertFalse(stateManager.apiSynced)

        stateManager = StateManager(factory.realmFactory, RegTest())
        assertTrue(stateManager.apiSynced)
    }

    @Test
    fun apiSynced_SetTrue() {
        stateManager.apiSynced = true

        assertTrue(stateManager.apiSynced)
    }

    @Test
    fun apiSynced_NotSet() {
        assertFalse(stateManager.apiSynced)
    }

    @Test
    fun apiSynced_Update() {
        stateManager.apiSynced = true
        assertTrue(stateManager.apiSynced)

        stateManager.apiSynced = false
        assertFalse(stateManager.apiSynced)
    }

}
