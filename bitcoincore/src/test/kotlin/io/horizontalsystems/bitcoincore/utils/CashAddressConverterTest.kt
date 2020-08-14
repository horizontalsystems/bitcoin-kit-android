package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CashAddressConverterTest : Spek({

    lateinit var converter: CashAddressConverter
    lateinit var address: Address

    val hrp = "bitcoincash"

    fun hashToAddress(hash: String, hrp: String, string: String, type: ScriptType) {
        converter = CashAddressConverter(hrp)
        address = converter.convert(hash.hexToByteArray(), type)

        assertEquals(string, address.string)
    }

    fun stringToAddress(addressString: String) {
        try {
            converter.convert(addressString)
            fail("Expected an Exception to be thrown")
        } catch (e: AddressFormatException) {

        } catch (e: Exception) {
            fail("Expected an AddressFormatException to be thrown")
        }
    }

    describe("CashAddressConverter") {

        it("convert_strings") {
            converter = CashAddressConverter(hrp)

            // empty string
            stringToAddress("")
            // invalid upper and lower case at the same time "Q" "zdvr2hn0xrz99fcp6hkjxzk848rjvvhgytv4fket8"
            stringToAddress("bitcoincash:Qzdvr2hn0xrz99fcp6hkjxzk848rjvvhgytv4fket8")
            // no prefix
            // stringToAddress("qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2")
            // invalid prefix "bitcoincash012345"
            stringToAddress("bitcoincash012345:qzdvr2hn0xrz99fcp6hkjxzk848rjvvhgytv4fket8")
            // invalid character "1"
            stringToAddress("bitcoincash:111112hn0xrz99fcp6hkjxzk848rjvvhgytv411111")
            // unexpected character "💦😆"
            stringToAddress("bitcoincash:qzdvr2hn0xrz99fcp6hkjxzk848rjvvhgytv4fket8💦😆")
            // invalid checksum
            stringToAddress("bitcoincash:zzzzz2hn0xrz99fcp6hkjxzk848rjvvhgytv4zzzzz")
        }

        it("convert_bytes") {

            // The following test cases are from the spec about cashaddr
            // https://github.com/bitcoincashorg/bitcoincash.org/blob/master/spec/cashaddr.md

            hashToAddress("F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9", "bitcoincash", "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2", ScriptType.P2PKH)

            hashToAddress("F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9", "bitcoincash", "bitcoincash:qr6m7j9njldwwzlg9v7v53unlr4jkmx6eylep8ekg2", ScriptType.P2PKH)
            hashToAddress("F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9", "bchtest", "bchtest:pr6m7j9njldwwzlg9v7v53unlr4jkmx6eyvwc0uz5t", ScriptType.P2SH)
            hashToAddress("F5BF48B397DAE70BE82B3CCA4793F8EB2B6CDAC9", "pref", "pref:pr6m7j9njldwwzlg9v7v53unlr4jkmx6ey65nvtks5", ScriptType.P2SH)

            hashToAddress("7ADBF6C17084BC86C1706827B41A56F5CA32865925E946EA", "bitcoincash", "bitcoincash:q9adhakpwzztepkpwp5z0dq62m6u5v5xtyj7j3h2ws4mr9g0", ScriptType.P2PKH)
            hashToAddress("7ADBF6C17084BC86C1706827B41A56F5CA32865925E946EA", "bchtest", "bchtest:p9adhakpwzztepkpwp5z0dq62m6u5v5xtyj7j3h2u94tsynr", ScriptType.P2SH)
            hashToAddress("7ADBF6C17084BC86C1706827B41A56F5CA32865925E946EA", "pref", "pref:p9adhakpwzztepkpwp5z0dq62m6u5v5xtyj7j3h2khlwwk5v", ScriptType.P2SH)

            hashToAddress("3A84F9CF51AAE98A3BB3A78BF16A6183790B18719126325BFC0C075B", "bitcoincash", "bitcoincash:qgagf7w02x4wnz3mkwnchut2vxphjzccwxgjvvjmlsxqwkcw59jxxuz", ScriptType.P2PKH)
            hashToAddress("3A84F9CF51AAE98A3BB3A78BF16A6183790B18719126325BFC0C075B", "bchtest", "bchtest:pgagf7w02x4wnz3mkwnchut2vxphjzccwxgjvvjmlsxqwkcvs7md7wt", ScriptType.P2SH)
            hashToAddress("3A84F9CF51AAE98A3BB3A78BF16A6183790B18719126325BFC0C075B", "pref", "pref:pgagf7w02x4wnz3mkwnchut2vxphjzccwxgjvvjmlsxqwkcrsr6gzkn", ScriptType.P2SH)

            hashToAddress("3173EF6623C6B48FFD1A3DCC0CC6489B0A07BB47A37F47CFEF4FE69DE825C060", "bitcoincash", "bitcoincash:qvch8mmxy0rtfrlarg7ucrxxfzds5pamg73h7370aa87d80gyhqxq5nlegake", ScriptType.P2PKH)
            hashToAddress("3173EF6623C6B48FFD1A3DCC0CC6489B0A07BB47A37F47CFEF4FE69DE825C060", "bchtest", "bchtest:pvch8mmxy0rtfrlarg7ucrxxfzds5pamg73h7370aa87d80gyhqxq7fqng6m6", ScriptType.P2SH)
            hashToAddress("3173EF6623C6B48FFD1A3DCC0CC6489B0A07BB47A37F47CFEF4FE69DE825C060", "pref", "pref:pvch8mmxy0rtfrlarg7ucrxxfzds5pamg73h7370aa87d80gyhqxq4k9m7qf9", ScriptType.P2SH)

            hashToAddress("C07138323E00FA4FC122D3B85B9628EA810B3F381706385E289B0B25631197D194B5C238BEB136FB", "bitcoincash", "bitcoincash:qnq8zwpj8cq05n7pytfmskuk9r4gzzel8qtsvwz79zdskftrzxtar994cgutavfklv39gr3uvz", ScriptType.P2PKH)
            hashToAddress("C07138323E00FA4FC122D3B85B9628EA810B3F381706385E289B0B25631197D194B5C238BEB136FB", "bchtest", "bchtest:pnq8zwpj8cq05n7pytfmskuk9r4gzzel8qtsvwz79zdskftrzxtar994cgutavfklvmgm6ynej", ScriptType.P2SH)
            hashToAddress("C07138323E00FA4FC122D3B85B9628EA810B3F381706385E289B0B25631197D194B5C238BEB136FB", "pref", "pref:pnq8zwpj8cq05n7pytfmskuk9r4gzzel8qtsvwz79zdskftrzxtar994cgutavfklv0vx5z0w3", ScriptType.P2SH)

            hashToAddress("E361CA9A7F99107C17A622E047E3745D3E19CF804ED63C5C40C6BA763696B98241223D8CE62AD48D863F4CB18C930E4C", "bitcoincash", "bitcoincash:qh3krj5607v3qlqh5c3wq3lrw3wnuxw0sp8dv0zugrrt5a3kj6ucysfz8kxwv2k53krr7n933jfsunqex2w82sl", ScriptType.P2PKH)
            hashToAddress("E361CA9A7F99107C17A622E047E3745D3E19CF804ED63C5C40C6BA763696B98241223D8CE62AD48D863F4CB18C930E4C", "bchtest", "bchtest:ph3krj5607v3qlqh5c3wq3lrw3wnuxw0sp8dv0zugrrt5a3kj6ucysfz8kxwv2k53krr7n933jfsunqnzf7mt6x", ScriptType.P2SH)
            hashToAddress("E361CA9A7F99107C17A622E047E3745D3E19CF804ED63C5C40C6BA763696B98241223D8CE62AD48D863F4CB18C930E4C", "pref", "pref:ph3krj5607v3qlqh5c3wq3lrw3wnuxw0sp8dv0zugrrt5a3kj6ucysfz8kxwv2k53krr7n933jfsunqjntdfcwg", ScriptType.P2SH)

            hashToAddress("D9FA7C4C6EF56DC4FF423BAAE6D495DBFF663D034A72D1DC7D52CBFE7D1E6858F9D523AC0A7A5C34077638E4DD1A701BD017842789982041", "bitcoincash", "bitcoincash:qmvl5lzvdm6km38lgga64ek5jhdl7e3aqd9895wu04fvhlnare5937w4ywkq57juxsrhvw8ym5d8qx7sz7zz0zvcypqscw8jd03f", ScriptType.P2PKH)
            hashToAddress("D9FA7C4C6EF56DC4FF423BAAE6D495DBFF663D034A72D1DC7D52CBFE7D1E6858F9D523AC0A7A5C34077638E4DD1A701BD017842789982041", "bchtest", "bchtest:pmvl5lzvdm6km38lgga64ek5jhdl7e3aqd9895wu04fvhlnare5937w4ywkq57juxsrhvw8ym5d8qx7sz7zz0zvcypqs6kgdsg2g", ScriptType.P2SH)
            hashToAddress("D9FA7C4C6EF56DC4FF423BAAE6D495DBFF663D034A72D1DC7D52CBFE7D1E6858F9D523AC0A7A5C34077638E4DD1A701BD017842789982041", "pref", "pref:pmvl5lzvdm6km38lgga64ek5jhdl7e3aqd9895wu04fvhlnare5937w4ywkq57juxsrhvw8ym5d8qx7sz7zz0zvcypqsammyqffl", ScriptType.P2SH)

            hashToAddress("D0F346310D5513D9E01E299978624BA883E6BDA8F4C60883C10F28C2967E67EC77ECC7EEEAEAFC6DA89FAD72D11AC961E164678B868AEEEC5F2C1DA08884175B", "bitcoincash", "bitcoincash:qlg0x333p4238k0qrc5ej7rzfw5g8e4a4r6vvzyrcy8j3s5k0en7calvclhw46hudk5flttj6ydvjc0pv3nchp52amk97tqa5zygg96mtky5sv5w", ScriptType.P2PKH)
            hashToAddress("D0F346310D5513D9E01E299978624BA883E6BDA8F4C60883C10F28C2967E67EC77ECC7EEEAEAFC6DA89FAD72D11AC961E164678B868AEEEC5F2C1DA08884175B", "bchtest", "bchtest:plg0x333p4238k0qrc5ej7rzfw5g8e4a4r6vvzyrcy8j3s5k0en7calvclhw46hudk5flttj6ydvjc0pv3nchp52amk97tqa5zygg96mc773cwez", ScriptType.P2SH)
            hashToAddress("D0F346310D5513D9E01E299978624BA883E6BDA8F4C60883C10F28C2967E67EC77ECC7EEEAEAFC6DA89FAD72D11AC961E164678B868AEEEC5F2C1DA08884175B", "pref", "pref:plg0x333p4238k0qrc5ej7rzfw5g8e4a4r6vvzyrcy8j3s5k0en7calvclhw46hudk5flttj6ydvjc0pv3nchp52amk97tqa5zygg96mg7pj3lh8", ScriptType.P2SH)
        }
    }
})
