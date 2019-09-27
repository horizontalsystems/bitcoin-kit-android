package io.horizontalsystems.groestlcoinkit.crypto;

import java.util.Arrays;

import io.horizontalsystems.bitcoincore.crypto.Base58;
import static io.horizontalsystems.groestlcoinkit.GroestlHasherKt.groestlhash;


public class GroestlcoinBase58 extends Base58 {

    /**
     * Decode a Base58-encoded checksummed string and verify the checksum.  The
     * checksum will then be removed from the decoded value.
     *
     * @param   string                      Base-58 encoded checksummed string
     * @return                              Decoded value
     * @throws  IllegalArgumentException    The string is not valid or the checksum is incorrect
     */
    public static byte[] decodeChecked(String string) throws IllegalArgumentException {
        //
        // Decode the string
        //
        byte[] decoded = decode(string);
        if (decoded.length < 4)
            throw new IllegalArgumentException("Decoded string is too short");
        //
        // Verify the checksum contained in the last 4 bytes
        //
        byte[] bytes = Arrays.copyOfRange(decoded, 0, decoded.length-4);
        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length-4, decoded.length);
        byte[] hash = Arrays.copyOfRange(groestlhash(bytes), 0, 4);
        if (!Arrays.equals(hash, checksum))
            throw new IllegalArgumentException("Checksum is not correct");
        //
        // Return the result without the checksum bytes
        //
        return bytes;
    }
}
