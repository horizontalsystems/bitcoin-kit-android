package io.horizontalsystems.bitcoincore.crypto;

import java.math.BigInteger;

import io.horizontalsystems.bitcoincore.utils.Utils;

public class CompactBits {

    /**
     * The "compact" format is a representation of a whole
     * number N using an unsigned 32bit number similar to a
     * floating point format.
     * The most significant 8 bits are the unsigned exponent of base 256.
     * This exponent can be thought of as "number of bytes of N".
     * The lower 23 bits are the mantissa.
     * Bit number 24 (0x800000) represents the sign of N.
     * N = (-1^sign) * mantissa * 256^(exponent-3)
     *
     * Satoshi's original implementation used BN_bn2mpi() and BN_mpi2bn().
     * MPI uses the most significant bit of the first byte as sign.
     * Thus 0x1234560000 is compact (0x05123456)
     * and  0xc0de000000 is compact (0x0600c0de)
     *
     * Bitcoin only uses this "compact" format for encoding difficulty
     * targets, which are unsigned 256bit quantities.  Thus, all the
     * complexities of the sign bit and using base 256 are probably an
     * implementation accident.
     */
    public static BigInteger decode(long compact) {
        int size = ((int) (compact >> 24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte) size;
        if (size >= 1) bytes[4] = (byte) ((compact >> 16) & 0xFF);
        if (size >= 2) bytes[5] = (byte) ((compact >> 8) & 0xFF);
        if (size >= 3) bytes[6] = (byte) (compact & 0xFF);

        return decodeMPI(bytes, true);
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function.
     * They consist of a 4 byte big endian length field,
     * followed by the stated number of bytes
     * representing the number in big endian format (with a sign bit).
     *
     * @param hasLength can be set to false if the given array is missing the 4 byte length field
     */
    public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
        byte[] buf;
        if (hasLength) {
            int length = (int) Utils.readUint32BE(mpi, 0);
            buf = new byte[length];
            System.arraycopy(mpi, 4, buf, 0, length);
        } else
            buf = mpi;
        if (buf.length == 0)
            return BigInteger.ZERO;
        boolean isNegative = (buf[0] & 0x80) == 0x80;
        if (isNegative)
            buf[0] &= 0x7f;
        BigInteger result = new BigInteger(buf);
        return isNegative ? result.negate() : result;
    }

    /**
     * @see CompactBits#decode
     */
    public static long encode(BigInteger value) {
        long result;
        int size = value.toByteArray().length;
        if (size <= 3)
            result = value.longValue() << 8 * (3 - size);
        else
            result = value.shiftRight(8 * (size - 3)).longValue();
        // The 0x00800000 bit denotes the sign.
        // Thus, if it is already set, divide the mantissa by 256 and increase the exponent.
        if ((result & 0x00800000L) != 0) {
            result >>= 8;
            size++;
        }
        result |= size << 24;
        result |= value.signum() == -1 ? 0x00800000 : 0;
        return result;
    }
}
