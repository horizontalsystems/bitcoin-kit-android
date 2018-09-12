package bitcoin.wallet.kit

import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing

object TestUtils {

    // Casting Null to Object Type. In Mockito anyObject() and any() will return Null
    fun <T> any() = Mockito.any<T>() as T

    // Alias method for Mockito.when()
    fun <T> whenever(methodCall: T): OngoingStubbing<T> {
        return Mockito.`when`(methodCall)
    }
}
