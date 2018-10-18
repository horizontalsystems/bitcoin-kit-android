package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.blocks.validators.TestnetValidator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(TestNet::class)

class TestNetTest {

    private val validator = Mockito.mock(TestnetValidator::class.java)
    private lateinit var network: TestNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(TestnetValidator::class.java)
                .withAnyArguments()
                .thenReturn(validator)

        network = TestNet()
    }

    @Test
    fun validate() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        network.validateBlock(block1, blockPrev)

        verify(validator).validate(any(), any())
    }

}
