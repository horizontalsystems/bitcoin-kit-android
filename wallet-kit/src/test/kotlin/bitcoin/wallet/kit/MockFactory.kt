package bitcoin.wallet.kit

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.headers.HeaderHandler
import bitcoin.wallet.kit.headers.HeaderSyncer
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.network.TestNet
import org.mockito.Mockito.mock

class MockFactory {

    val realmFactory = mock(RealmFactory::class.java)
    val peerGroup = mock(PeerGroup::class.java)
    val network = mock(TestNet::class.java)
    val headerSyncer = mock(HeaderSyncer::class.java)
    val headerHandler = mock(HeaderHandler::class.java)

}
