package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.AddressType
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SegwitAddressConverterTest : Spek({

    lateinit var converter: SegwitAddressConverter
    lateinit var bytes: ByteArray
    lateinit var program: ByteArray
    lateinit var addressString: String
    lateinit var address: Address

    describe("#convert") {
        it("P2WPKH") {
            addressString = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
            program = "751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()
            bytes = "0014".hexToByteArray() + program

            converter = SegwitAddressConverter("bc")
            address = converter.convert(bytes, ScriptType.P2WPKH)

            assertEquals(AddressType.WITNESS, address.type)
            assertEquals(addressString, address.string)
            assertArrayEquals(program, address.hash)
        }

        it("P2WSH") {
            addressString = "tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7"
            program = "1863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262".hexToByteArray()
            bytes = "00201863143c14c5166804bd19203356da136c985678cd4d27a1b8c6329604903262".hexToByteArray()

            converter = SegwitAddressConverter("tb")
            address = converter.convert(bytes, ScriptType.P2WSH)

            assertEquals(AddressType.WITNESS, address.type)
            assertEquals(addressString, address.string)
            assertArrayEquals(program, address.hash)
        }

        it("witness1") {
            addressString = "bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7k7grplx"
            program = "751e76e8199196d454941c45d1b3a323f1433bd6751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()
            bytes = "5128751e76e8199196d454941c45d1b3a323f1433bd6751e76e8199196d454941c45d1b3a323f1433bd6".hexToByteArray()

            converter = SegwitAddressConverter("bc")
            address = converter.convert(bytes, ScriptType.P2WPKH)

            assertEquals(AddressType.WITNESS, address.type)
            assertEquals(addressString, address.string)
            assertArrayEquals(program, address.hash)
        }
    }
})
