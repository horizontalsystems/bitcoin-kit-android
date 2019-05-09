/*
 * Copyright 2018 Coinomi Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.horizontalsystems.bitcoincore.crypto;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;

import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException;

public class Bech32Segwit extends Bech32 {

    /** Find the polynomial with value coefficients mod the generator as 30-bit. */
    private static int polymod(final byte[] values) {
        int c = 1;
        for (byte v_i : values) {
            int c0 = (c >>> 25) & 0xff;
            c = ((c & 0x1ffffff) << 5) ^ (v_i & 0xff);
            if ((c0 & 1) != 0) c ^= 0x3b6a57b2;
            if ((c0 & 2) != 0) c ^= 0x26508e6d;
            if ((c0 & 4) != 0) c ^= 0x1ea119fa;
            if ((c0 & 8) != 0) c ^= 0x3d4233dd;
            if ((c0 & 16) != 0) c ^= 0x2a1462b3;
        }
        return c;
    }

    /** Expand a address prefix for use in checksum computation. */
    private static byte[] expandPrefix(final String prefix) {
        int prefixLength = prefix.length();
        byte ret[] = new byte[prefixLength * 2 + 1];
        for (int i = 0; i < prefixLength; ++i) {
            int c = prefix.charAt(i) & 0x7f; // Limit to standard 7-bit ASCII
            ret[i] = (byte) ((c >>> 5) & 0x07);
            ret[i + prefixLength + 1] = (byte) (c & 0x1f);
        }
        ret[prefixLength] = 0;
        return ret;
    }

    /** Verify a checksum. */
    private static boolean verifyChecksum(final String prefix, final byte[] values) {
        byte[] prefixExpanded = expandPrefix(prefix);
        byte[] combined = new byte[prefixExpanded.length + values.length];
        System.arraycopy(prefixExpanded, 0, combined, 0, prefixExpanded.length);
        System.arraycopy(values, 0, combined, prefixExpanded.length, values.length);
        return polymod(combined) == 1;
    }

    /** Create a checksum. */
    private static byte[] createChecksum(final String prefix, final byte[] values) {
        byte[] prefixExpanded = expandPrefix(prefix);
        byte[] enc = new byte[prefixExpanded.length + values.length + 6];
        System.arraycopy(prefixExpanded, 0, enc, 0, prefixExpanded.length);
        System.arraycopy(values, 0, enc, prefixExpanded.length, values.length);
        int mod = polymod(enc) ^ 1;
        byte[] ret = new byte[6];
        for (int i = 0; i < 6; ++i) {
            ret[i] = (byte) ((mod >>> (5 * (5 - i))) & 31);
        }
        return ret;
    }

    /** Encode a Bech32 string. */
    public static String encode(final Bech32Data bech32) throws AddressFormatException {
        return encode(bech32.hrp, bech32.data);
    }

    /** Encode a Bech32 string. */
    public static String encode(String prefix, final byte[] values) throws AddressFormatException {
        if (prefix.length() < 1) throw new AddressFormatException("Human-readable part is too short");
        if (prefix.length() > 83) throw new AddressFormatException("Human-readable part is too long");
        prefix = prefix.toLowerCase(Locale.ROOT);
        byte[] checksum = createChecksum(prefix, values);
        byte[] combined = new byte[values.length + checksum.length];
        System.arraycopy(values, 0, combined, 0, values.length);
        System.arraycopy(checksum, 0, combined, values.length, checksum.length);
        StringBuilder sb = new StringBuilder(prefix.length() + 1 + combined.length);
        sb.append(prefix);
        sb.append('1');
        for (byte b : combined) {
            sb.append(CHARSET.charAt(b));
        }
        return sb.toString();
    }

    /** Decode a Bech32 string. */
    public static Bech32Data decode(final String str) throws AddressFormatException {
        boolean lower = false, upper = false;
        if (str.length() < 8) throw new AddressFormatException("Input too short");
        if (str.length() > 90) throw new AddressFormatException("Input too long");
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < 33 || c > 126) throw new AddressFormatException("Characters out of range");
            if (c >= 'a' && c <= 'z') lower = true;
            if (c >= 'A' && c <= 'Z') upper = true;
        }
        if (lower && upper) throw new AddressFormatException("Cannot mix upper and lower cases");
        int pos = str.lastIndexOf('1');
        if (pos < 1) throw new AddressFormatException("Missing human-readable part");
        if (pos + 7 > str.length()) throw new AddressFormatException("Data part too short");
        byte[] values = new byte[str.length() - 1 - pos];
        for (int i = 0; i < str.length() - 1 - pos; ++i) {
            char c = str.charAt(i + pos + 1);
            if (CHARSET_REV[c] == -1) throw new AddressFormatException("Characters out of range");
            values[i] = CHARSET_REV[c];
        }
        String prefix = str.substring(0, pos).toLowerCase(Locale.ROOT);
        if (!verifyChecksum(prefix, values)) throw new AddressFormatException("Invalid checksum");
        return new Bech32Data(prefix, Arrays.copyOfRange(values, 0, values.length - 6));
    }

    /** General power-of-2 base conversion */
    public static byte[] convertBits(final byte[] data, final int start, final int size, final int fromBits, final int toBits, final boolean pad) throws AddressFormatException {
        int acc = 0;
        int bits = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        final int maxv = (1 << toBits) - 1;
        final int max_acc = (1 << (fromBits + toBits - 1)) - 1;
        for (int i = 0; i < size; i++) {
            int value = data[i + start] & 0xff;
            if ((value >>> fromBits) != 0) {
                throw new AddressFormatException("Invalid data range: data[" + i + "]=" + value + " (fromBits=" + fromBits + ")");
            }
            acc = ((acc << fromBits) | value) & max_acc;
            bits += fromBits;
            while (bits >= toBits) {
                bits -= toBits;
                out.write((acc >>> bits) & maxv);
            }
        }
        if (pad) {
            if (bits > 0)
                out.write((acc << (toBits - bits)) & maxv);
        } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
            throw new AddressFormatException("Could not convert bits, invalid padding");
        }
        return out.toByteArray();
    }
}
