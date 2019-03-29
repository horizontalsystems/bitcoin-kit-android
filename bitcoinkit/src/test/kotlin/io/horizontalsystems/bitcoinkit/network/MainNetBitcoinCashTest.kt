package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.any
import io.horizontalsystems.bitcoinkit.blocks.validators.BitcoinCashValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(MainNetBitcoinCash::class)

class MainNetBitcoinCashTest {
    private val storage = mock(IStorage::class.java)
    private val block1 = mock(Block::class.java)
    private val block2 = mock(Block::class.java)
    private val blockValidator = mock(BitcoinCashValidator::class.java)

    lateinit var network: MainNetBitcoinCash

    @Before
    fun setup() {
        PowerMockito
                .whenNew(BitcoinCashValidator::class.java)
                .withAnyArguments()
                .thenReturn(blockValidator)

        network = MainNetBitcoinCash(storage)
    }

    @Test
    fun validate() {
        network.validateBlock(block1, block2)

        verify(blockValidator).validate(any(), any())
    }

}
