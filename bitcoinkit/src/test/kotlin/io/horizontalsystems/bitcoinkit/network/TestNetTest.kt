package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(TestNet::class)

class TestNetTest {

    private var storage = mock(IStorage::class.java)
    private var block1 = mock(Block::class.java)
    private var block2 = mock(Block::class.java)

    private val validator = mock(TestnetValidator::class.java)
    private lateinit var network: TestNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(TestnetValidator::class.java)
                .withAnyArguments()
                .thenReturn(validator)

        network = TestNet(storage)
    }

    @Test
    fun validate() {
        network.validateBlock(block1, block2)

        verify(validator).validate(block1, block2)
    }

}
