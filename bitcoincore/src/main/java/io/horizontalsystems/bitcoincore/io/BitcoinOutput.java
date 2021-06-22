package io.horizontalsystems.bitcoincore.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Output "stream" for bitcoin protocol.
 *
 * @author Michael Liao
 *
 */
public final class BitcoinOutput {

    private UnsafeByteArrayOutputStream out;

    /**
     * Creates an instance of UnsafeByteArrayOutputStream of size 1024.
     */
    public BitcoinOutput() {
        this.out = new UnsafeByteArrayOutputStream(1024);
    }

    /**
     * Writes a byte array to the OutputStream.
     * @param bytes Byte array that's to be written.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput write(byte[] bytes) {
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    /**
     * Writes a byte to the OutputStream.
     * @param v The byte to be written.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeByte(int v) {
        out.write(v);
        return this;
    }
    /**
     * Writes a short to the OutputStream.
     * @param v The short to be written.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeShort(short v) {
        out.write(0xff & v);
        out.write(0xff & (v >> 8));
        return this;
    }
    /**
     * Writes an int to the OutputStream.
     * @param v Int value.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeInt(int v) {
        out.write(0xff & v);
        out.write(0xff & (v >> 8));
        out.write(0xff & (v >> 16));
        out.write(0xff & (v >> 24));
        return this;
    }
    /**
     * Writes a 32-bit int to the OutputStream.
     * @param v Long value to be converted.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeInt32(long v) {
        out.write((int)(0xff & v));
        out.write((int)(0xff & (v >> 8)));
        out.write((int)(0xff & (v >> 16)));
        out.write((int)(0xff & (v >> 24)));
        return this;
    }

    /**
     * Writes a Long to the OutputStream.
     * @param v Long value to be converted.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeLong(long v) {
        out.write((int) (0xff & v));
        out.write((int) (0xff & (v >> 8)));
        out.write((int) (0xff & (v >> 16)));
        out.write((int) (0xff & (v >> 24)));
        out.write((int) (0xff & (v >> 32)));
        out.write((int) (0xff & (v >> 40)));
        out.write((int) (0xff & (v >> 48)));
        out.write((int) (0xff & (v >> 56)));
        return this;
    }

    /**
     * Writes a var int to the OutputStream.
     * @param n Long value to be converted.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeVarInt(long n) {
        if (n < 0xfd) {
            writeByte((int) n);
        } else if (n <= 0xffff) {
            writeByte(0xfd);
            writeByte((int) (n & 0xff));
            writeByte((int) ((n >> 8) & 0xff));
        } else if (n <= 0xffffffff) {
            writeByte(0xfe);
            writeInt((int) n);
        } else {
            writeByte(0xff);
            writeLong(n);
        }
        return this;
    }

    /**
     *  Writes an unsigned int to the OutputStream.
     * @param ln Long value to be converted.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeUnsignedInt(long ln) {
        int n = (int) (0xffffffff & ln);
        writeInt(n);
        return this;
    }

    /**
     * Writes an unsigned short to the OutputStream.
     * @param i integer value to be casted as an unsigned short.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeUnsignedShort(int i) {
        short n = (short) (0xffff & i);
        writeShort(n);
        return this;
    }

    /**
     * Writes a String to the OutputStream.
     * @param str String value to be written.
     * @return The newly written OutputStream.
     */
    public BitcoinOutput writeString(String str) {
        byte[] bs = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bs.length);
        write(bs);
        return this;
    }

    public byte[] toByteArray() {
        return out.toByteArray();
    }

}
