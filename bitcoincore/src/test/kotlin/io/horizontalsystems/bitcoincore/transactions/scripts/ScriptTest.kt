package io.horizontalsystems.bitcoincore.transactions.scripts

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ScriptTest : Spek({
    lateinit var script: Script

    describe("init") {

        it("scriptSig") {
            script = Script("47304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701410414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c".hexToByteArray())

            assertEquals("PUSHDATA(71)[304402202b4da291cc39faf8433911988f9f49fc5c995812ca2f94db61468839c228c3e90220628bff3ff32ec95825092fa051cba28558a981fcf59ce184b14f2e215e69106701] PUSHDATA(65)[0414b38f4be3bb9fa0f4f32b74af07152b2f2f630bc02122a491137b6c523e46f18a0d5034418966f93dfc37cc3739ef7b2007213a302b7fba161557f4ad644a1c]", script.toString())
        }


        it("payToPKH") {
            script = Script("76a91433e81a941e64cda12c6a299ed322ddbdd03f8d0e88ac".hexToByteArray())

            assertEquals("DUP HASH160 PUSHDATA(20)[33e81a941e64cda12c6a299ed322ddbdd03f8d0e] EQUALVERIFY CHECKSIG", script.toString())
        }


        it("payToPK") {
            script = Script("210378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6ac".hexToByteArray())

            assertEquals("PUSHDATA(33)[0378e9dc79ff921df6c3f94d440fb011c65ed03586b1dd01c317934a4f00f251e6] CHECKSIG", script.toString())
        }


        it("payToWPKH") {
            script = Script("0014799d283e7f92af1dd242bf4eea513c6efd117de2".hexToByteArray())

            assertEquals("0[] PUSHDATA(20)[799d283e7f92af1dd242bf4eea513c6efd117de2]", script.toString())
        }


        it("payToWSH") {
            script = Script("0020a99d08fbec6958f4d4a3776c3728ec448934d25fe1142054b8b68bac866dfc3a".hexToByteArray())

            assertEquals("0[] PUSHDATA(32)[a99d08fbec6958f4d4a3776c3728ec448934d25fe1142054b8b68bac866dfc3a]", script.toString())
        }


        it("payToSH") {
            script = Script("a9144b60b6bcf50bf637fe66c3da5c11524cb3ab971187".hexToByteArray())

            assertEquals("HASH160 PUSHDATA(20)[4b60b6bcf50bf637fe66c3da5c11524cb3ab9711] EQUAL", script.toString())
        }


        it("payFrom_PKH") {
            val pk = "03b1ae868b76e84f8ae1cb3ad958653d8a23b444e8639c0c0f00e8de27541cb977"
            script = Script("4830450221009b1fc7f43826c4c61bb0bc7c9f667c7383f728647563f19f75db6ca701f4326f02205bc4dd125ffe4e903a59c87ed4362abb86acbf2b1e6fd8021cfc6d4f8384b0e50121$pk".hexToByteArray())

            assertEquals("PUSHDATA(72)[30450221009b1fc7f43826c4c61bb0bc7c9f667c7383f728647563f19f75db6ca701f4326f02205bc4dd125ffe4e903a59c87ed4362abb86acbf2b1e6fd8021cfc6d4f8384b0e501] PUSHDATA(33)[03b1ae868b76e84f8ae1cb3ad958653d8a23b444e8639c0c0f00e8de27541cb977]", script.toString())
        }

        // tx: 761cc7102efe24f4353ae7dc816fbed5e15963d11ca93e36449d521bda21ac4d
        it("payFrom_SH") {
            script = Script("004830450221008c203a0881f75c731d9a3a2e6d2ffa37da7095b7dde61a9e7a906659219cd0fa02202677097ca7f7e164f73924fe8f84e1e6fc6611450efcda360ce771e98af9f73d0147304402201cba9b641483476f67a4cef08d7280f51de8d7615fcce76642d944dc07132a990220323d13175477bbf67c8c36fb243bec0e4c410bc9173a186d9f8e98ce3445363601475221025b64f7c63e30f315259393f64dcca269d18386997b1cc93da1388c4021e3ea8e210386d42d5d7027ac08ddcbb066e2140575091fe7dc1d202a008eb5e036725e975652ae".hexToByteArray())

            assertEquals("0[] PUSHDATA(72)[30450221008c203a0881f75c731d9a3a2e6d2ffa37da7095b7dde61a9e7a906659219cd0fa02202677097ca7f7e164f73924fe8f84e1e6fc6611450efcda360ce771e98af9f73d01] PUSHDATA(71)[304402201cba9b641483476f67a4cef08d7280f51de8d7615fcce76642d944dc07132a990220323d13175477bbf67c8c36fb243bec0e4c410bc9173a186d9f8e98ce3445363601] PUSHDATA(71)[5221025b64f7c63e30f315259393f64dcca269d18386997b1cc93da1388c4021e3ea8e210386d42d5d7027ac08ddcbb066e2140575091fe7dc1d202a008eb5e036725e975652ae]", script.toString())
        }


        it("payFrom_WPKH") {
            script = Script("483045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb19190121038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac".hexToByteArray())
            assertEquals("PUSHDATA(72)[3045022100df7b7e5cda14ddf91290e02ea10786e03eb11ee36ec02dd862fe9a326bbcb7fd02203f5b4496b667e6e281cc654a2da9e4f08660c620a1051337fa8965f727eb191901] PUSHDATA(33)[038262a6c6cec93c2d3ecd6c6072efea86d02ff8e3328bbd0242b20af3425990ac]", script.toString())
        }
    }
})
