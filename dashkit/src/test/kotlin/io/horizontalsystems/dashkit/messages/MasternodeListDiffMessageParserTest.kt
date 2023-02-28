package io.horizontalsystems.dashkit.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import org.junit.jupiter.api.Assertions
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

class MasternodeListDiffMessageParserTest : Spek({
    val messageParser = MasternodeListDiffMessageParser()

    describe("#parseMessage") {
        it("parses successfully") {
            val resource = MasternodeListDiffMessageParserTest::class.java.getResource("messages/mnlistdiff.bin")
            val payload = File(resource!!.path).readBytes()

            Assertions.assertDoesNotThrow {
                messageParser.parseMessage(BitcoinInputMarkable(payload))
            }
        }
    }
})
