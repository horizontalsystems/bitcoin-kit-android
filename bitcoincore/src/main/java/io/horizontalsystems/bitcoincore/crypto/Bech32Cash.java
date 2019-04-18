
package io.horizontalsystems.bitcoincore.crypto;

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException;

public class Bech32Cash extends Bech32 {

    /**
     * This function will compute what 8 5-bit values to XOR into the last 8 input
     * values, in order to make the checksum 0. These 8 values are packed together
     * in a single 40-bit integer. The higher bits correspond to earlier values.
     */
    private static long polymod(final byte[] v) {

        long c = 1;
        for (byte d : v) {
            // First, determine the value of c0:
            byte c0 = (byte) (c >> 35);

            // Then compute c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d:
            c = ((c & 0x07ffffffffL) << 5) ^ d;

            // Finally, for each set bit n in c0, conditionally add {2^n}k(x):
            if ((c0 & 0x01) != 0) c ^= 0x98f2bc8e61L;
            if ((c0 & 0x02) != 0) c ^= 0x79b76d99e2L;
            if ((c0 & 0x04) != 0) c ^= 0xf33e5fb3c4L;
            if ((c0 & 0x08) != 0) c ^= 0xae2eabe2a8L;
            if ((c0 & 0x10) != 0) c ^= 0x1e4f43e470L;
        }

        // polymod computes what value to xor into the final values to make the
        // checksum 0. However, if we required that the checksum was 0, it would be
        // the case that appending a 0 to a valid list of values would result in a
        // new valid list. For that reason, cashaddr requires the resulting checksum
        // to be 1 instead.
        return c ^ 1;
    }

    /** Convert to lower case. Assume the input is a character. */
    static char lowerCase(char c) {
        // ASCII black magic.
        return (char) (c | 0x20);
    }

    /** Expand the address prefix for the checksum computation. */
    static byte[] expandPrefix(String prefix) {
        byte[] ret = new byte[prefix.length() + 1];
        byte[] prefixBytes = prefix.getBytes();

        for (int i = 0; i < prefix.length(); ++i) {
            ret[i] = (byte) (prefixBytes[i] & 0x1f);
        }

        ret[prefix.length()] = 0;
        return ret;
    }

    /** Verify a checksum. */
    static boolean verifyChecksum(String prefix, byte[] payload) {
        return polymod(cat(expandPrefix(prefix), payload)) == 0;
    }

    /** Create a checksum. */
    static byte[] createChecksum(String prefix, final byte[] payload) {
        byte[] enc = cat(expandPrefix(prefix), payload);
        // Append 8 zeroes.
        byte[] enc2 = new byte[enc.length + 8];
        System.arraycopy(enc, 0, enc2, 0, enc.length);
        // Determine what to XOR into those 8 zeroes.
        long mod = polymod(enc2);
        byte[] ret = new byte[8];
        for (int i = 0; i < 8; ++i) {
            // Convert the 5-bit groups in mod to checksum values.
            ret[i] = (byte) ((mod >> (5 * (7 - i))) & 0x1f);
        }

        return ret;
    }

    /** Encode a cashaddr string. */
    public static String encode(String prefix, byte[] payload) {
        byte[] checksum = createChecksum(prefix, payload);
        byte[] combined = cat(payload, checksum);
        StringBuilder ret = new StringBuilder(prefix + ':');

        //ret.setLength(ret.length() + combined.length);
        for (byte c : combined) {
            ret.append(CHARSET.charAt(c));
        }

        return ret.toString();
    }

    /**  Decode a cashaddr string. */
    public static Bech32Data decode(String str, String defaultPrefix) {
        // Go over the string and do some sanity checks.
        boolean lower = false, upper = false, hasNumber = false;
        int prefixSize = 0;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c >= 'a' && c <= 'z') {
                lower = true;
                continue;
            }

            if (c >= 'A' && c <= 'Z') {
                upper = true;
                continue;
            }

            if (c >= '0' && c <= '9') {
                // We cannot have numbers in the prefix.
                hasNumber = true;
                continue;
            }

            if (c == ':') {
                // The separator cannot be the first character, cannot have number
                // and there must not be 2 separators.
                if (hasNumber || i == 0 || prefixSize != 0) {
                    throw new AddressFormatException("cashaddr:  " + str + ": The separator cannot be the first character, cannot have number and there must not be 2 separators");
                }

                prefixSize = i;
                continue;
            }

            // We have an unexpected character.
            throw new AddressFormatException("cashaddr:  " + str + ": Unexpected character at pos " + i);
        }

        // We can't have both upper case and lowercase.
        if (upper && lower) {
            throw new AddressFormatException("cashaddr:  " + str + ": Cannot contain both upper and lower case letters");
        }

        // Get the prefix.
        StringBuilder prefix;
        if (prefixSize == 0) {
            prefix = new StringBuilder(defaultPrefix);
        } else {
            prefix = new StringBuilder(str.substring(0, prefixSize).toLowerCase());

            // Now add the ':' in the size.
            prefixSize++;
        }

        // Decode values.
        final int valuesSize = str.length() - prefixSize;
        byte[] values = new byte[valuesSize];
        for (int i = 0; i < valuesSize; ++i) {
            char c = str.charAt(i + prefixSize);
            // We have an invalid char in there.
            if (c > 127 || CHARSET_REV[c] == -1) {
                throw new AddressFormatException("cashaddr:  " + str + ": Unexpected character at pos " + i);
            }

            values[i] = CHARSET_REV[c];
        }

        // Verify the checksum.
        if (!verifyChecksum(prefix.toString(), values)) {
            throw new AddressFormatException("cashaddr:  " + str + ": Invalid Checksum ");
        }

        byte[] result = new byte[values.length - 8];
        System.arraycopy(values, 0, result, 0, values.length - 8);

        return new Bech32Data(prefix.toString(), result);
    }

    /**
     * Convert from one power-of-2 number base to another.
     *
     * If padding is enabled, this always return true. If not, then it returns true
     * of all the bits of the input are encoded in the output.
     */
    public static boolean convertBits(byte[] out, byte[] it, int fromBits, int toBits, boolean pad) {
        int acc = 0;
        int bits = 0;
        final int maxv = (1 << toBits) - 1;
        final int max_acc = (1 << (fromBits + toBits - 1)) - 1;
        int x = 0;
        for (int i = 0; i < it.length; ++i) {
            acc = ((acc << fromBits) | (it[i] & 0xff)) & max_acc;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out[x] = (byte) ((acc >> bits) & maxv);
                ++x;
            }
        }

        // We have remaining bits to encode but do not pad.
        if (!pad && bits != 0) {
            return false;
        }

        // We have remaining bits to encode so we do pad.
        if (pad && bits != 0) {
            out[x] = (byte) ((acc << (toBits - bits)) & maxv);
            ++x;
        }

        return true;
    }

    /** Concatenate two byte arrays. */
    private static byte[] cat(final byte[] x, final byte[] y) {
        byte[] z = new byte[x.length + y.length];
        System.arraycopy(x, 0, z, 0, x.length);
        System.arraycopy(y, 0, z, x.length, y.length);
        return z;
    }
}
