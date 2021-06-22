/**
 * Copyright 2011 Google Inc.
 * Copyright 2013-2016 Ronald W Hoffman
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
package io.horizontalsystems.bitcoincore.utils;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Static utility methods
 */
public class Utils {
    /** Bit masks (Low-order bit is bit 0 and high-order bit is bit 7) */
    private static final int bitMask[] = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};

    /** Strong random number generator */
    private static final Random rnd = new SecureRandom();

    /** Instance of a SHA-256 digest which we will use as needed */
    private static final MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    /**
     * Calculate the SHA-256 hash of the input and then hash the resulting hash again
     *
     * @param       input           Data to be hashed
     * @return                      The hash digest
     */
    public static byte[] doubleDigest(byte[] input) {
        return doubleDigest(input, 0, input.length);
    }

    /**
     * Calculate the SHA-256 hash of the input and then hash the resulting hash again
     *
     * @param       input           Data to be hashed
     * @param       offset          Starting offset within the data
     * @param       length          Number of data bytes to hash
     * @return                      The hash digest
     */
    public static byte[] doubleDigest(byte[] input, int offset, int length) {
        byte[] bytes;
        synchronized (digest) {
            digest.reset();
            digest.update(input, offset, length);
            byte[] first = digest.digest();
            bytes = digest.digest(first);
        }
        return bytes;
    }

    /**
     * Calculate RIPEMD160(SHA256(input)).  This is used in Address calculations.
     *
     * @param       input           The byte array to be hashed
     * @return                      The hashed result
     */
    public static byte[] sha256Hash160(byte[] input) {
        byte[] out = new byte[20];
        synchronized(digest) {
            digest.reset();
            byte[] sha256 = digest.digest(input);
            RIPEMD160Digest rDigest = new RIPEMD160Digest();
            rDigest.update(sha256, 0, sha256.length);
            rDigest.doFinal(out, 0);
        }
        return out;
    }

    /**
     * Calculate the HMAC-SHA512 digest for use with BIP 32
     *
     * @param       key             Key
     * @param       input           Bytes to be hashed
     * @return                      Hashed result
     */
    public static byte[] hmacSha512(byte[] key, byte[] input) {
        HMac hmac = new HMac(new SHA512Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(input, 0, input.length);
        byte[] out = new byte[64];
        hmac.doFinal(out, 0);
        return out;
    }

    /**
     * Checks if the specified bit is set
     *
     * @param       data            Byte array to check
     * @param       index           Bit position
     * @return                      TRUE if the bit is set
     */
    public static boolean checkBitLE(byte[] data, int index) {
        return (data[index>>>3] & bitMask[7&index]) != 0;
    }

    /**
     * Sets the specified bit
     * @param       data            Byte array
     * @param       index           Bit position
     */
    public static void setBitLE(byte[] data, int index) {
        data[index >>> 3] |= bitMask[7 & index];
    }

    /**
     * Calculate SHA256(SHA256(byte range 1 + byte range 2)).
     *
     * @param       input1          First input byte array
     * @param       offset1         Starting position in the first array
     * @param       length1         Number of bytes to process in the first array
     * @param       input2          Second input byte array
     * @param       offset2         Starting position in the second array
     * @param       length2         Number of bytes to process in the second array
     * @return                      The SHA-256 digest
     */
    public static byte[] doubleDigestTwoBuffers(byte[]input1, int offset1, int length1,
                                                byte[]input2, int offset2, int length2) {
        byte[] bytes;
        synchronized (digest) {
            digest.reset();
            digest.update(input1, offset1, length1);
            digest.update(input2, offset2, length2);
            byte[]first = digest.digest();
            bytes = digest.digest(first);
        }
        return bytes;
    }

    /**
     * Form a long value from a 4-byte array in big-endian format
     *
     * @param       bytes           The byte array
     * @param       offset          Starting offset within the array
     * @return                      The long value
     */
    public static long readUint32BE(byte[] bytes, int offset) {
        return (((long) bytes[offset++] & 0x00FFL) << 24) |
                (((long) bytes[offset++] & 0x00FFL) << 16) |
                (((long) bytes[offset++] & 0x00FFL) << 8) |
                ((long) bytes[offset] & 0x00FFL);
    }

    /** Parse 2 bytes from the stream as unsigned 16-bit integer in little endian format.
     * @param       is       InputStream
     * @return               The int value
     */
    public static int readUint16FromStream(InputStream is) {
        try {
            return (is.read() & 0xff) |
                    ((is.read() & 0xff) << 8);
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    /** Parse 4 bytes from the stream as unsigned 32-bit integer in little endian format.
     * @param       is InputStream
     * @return         The long value
     */
    public static long readUint32FromStream(InputStream is) {
        try {
            return (is.read() & 0xffl) |
                    ((is.read() & 0xffl) << 8) |
                    ((is.read() & 0xffl) << 16) |
                    ((is.read() & 0xffl) << 24);
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
    /**
     * Convert int to byte array
     *
     * @param       value Arbitrary int value
     * @return            Converted int
     */
    public static byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
    }
    /**
     * Convert int to byte array
     *
     * @param       data        Arbitrary byte array
     * @return                  Converted byte array
     */
    public static int byteArrayToUInt16LE(byte[] data) {
        return (data[0] & 0xff) | ((data[1] & 0xff) << 8);
    }

    /**
     * Parse an int value from bytes
     *
     * @param       b1      Arbitrary byte
     * @param       b2      Arbitrary byte
     * @param       b3      Arbitrary byte
     * @param       b4      Arbitrary byte
     * @return              Parsed int value
     */
    public static int intFromBytes(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 255) << 16 | (b3 & 255) << 8 | b4 & 255;
    }

    /** Generate random long number */
    public static long randomLong() {
        return (long) (rnd.nextDouble() * Long.MAX_VALUE);
    }

    /** Generate random number */
    public static int randomInt() {
        return (int) (rnd.nextDouble() * Integer.MAX_VALUE);
    }
}
