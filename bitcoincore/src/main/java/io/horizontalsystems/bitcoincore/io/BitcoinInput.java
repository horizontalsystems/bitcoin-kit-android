package io.horizontalsystems.bitcoincore.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Input "stream" for bitcoin protocol.
 *
 * @author Michael Liao
 */
public class BitcoinInput implements AutoCloseable {

    private static byte[] EMPTY_BYTES = new byte[0];
    protected final InputStream in;
    private byte[] bufferOf8bytes = new byte[8];

    public BitcoinInput(InputStream in) {
        this.in = in;
    }

    /**
     * Read btc var int (1~4 bytes). Reference:
     * https://en.bitcoin.it/wiki/Protocol_documentation#Variable_length_integer
     *
     * @return long value as int.
     * @throws IOException
     */
    public long readVarInt() throws IOException {
        byte[] buffer = new byte[1];
        if (in.read(buffer) < 0) {
            throw new EOFException();
        }
        int ch = 0xff & buffer[0];
        if (ch < 0xfd) {
            return ch;
        }
        if (ch == 0xfd) {
            int ch1 = in.read();
            int ch2 = in.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            return (ch2 << 8) + (ch1 << 0);
        }
        if (ch == 0xfe) {
            return readInt();
        }
        return readLong();
    }

    /**
     * Read next byte of data.
     *
     * @return int value as byte, or -1 if EOF.
     * @throws IOException
     */
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Read and fill bytes into byte array.
     *
     * @param b Arbitrary byte array
     * @throws IOException
     */
    public void readFully(byte b[]) throws IOException {
        int off = 0;
        int len = b.length;
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
        }
    }

    /**
     * Read and castes the read value to byte
     * @return The converted int
     * @throws IOException
     */
    public byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) (ch);
    }
    /**
     * Read and return
     * @return The read value
     * @throws IOException
     */
    public int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }
    /**
     * Read and create a short
     *
     * @return Converted Byte Array
     * @throws IOException
     */
    public short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch2 << 8) + (ch1 << 0));
    }
    /**
     * Read and create an unsigned short
     *
     * @return Converted Byte Array
     * @throws IOException
     */
    public int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (ch2 << 8) + (ch1 << 0);
    }

    public char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (char) ((ch2 << 8) + (ch1 << 0));
    }
    /**
     * Read and create an int
     *
     * @return Converted Byte Array
     * @throws IOException
     */
    public int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }
    /**
     * Read and create an unsigned int
     *
     * @return Converted Byte Array
     * @throws IOException
     */
    public long readUnsignedInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        long ln4 = ch4 & 0x00000000ffffffffL;
        return (ln4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);
    }
    /**
     * Read and create a long
     *
     * @return Converted Byte Array
     * @throws IOException
     */
    public long readLong() throws IOException {
        readFully(bufferOf8bytes);
        return (((long) bufferOf8bytes[7] << 56) + ((long) (bufferOf8bytes[6] & 255) << 48)
                + ((long) (bufferOf8bytes[5] & 255) << 40) + ((long) (bufferOf8bytes[4] & 255) << 32)
                + ((long) (bufferOf8bytes[3] & 255) << 24) + ((bufferOf8bytes[2] & 255) << 16)
                + ((bufferOf8bytes[1] & 255) << 8) + ((bufferOf8bytes[0] & 255) << 0));
    }

    /**
     * Read and create a String
     *
     * @return Converted Byte Array with Charset UTF-8 format
     * @throws IOException
     */
    public String readString() throws IOException {
        long len = readVarInt();
        if (len == 0) {
            return "";
        }
        byte[] buffer = new byte[(int) len];
        readFully(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    /**
     * Read and put values into a byte array
     *
     * @param len The desired length of the byte array
     * @return A byte array with a length of len
     * @throws IOException
     */
    public byte[] readBytes(int len) throws IOException {
        if (len == 0) {
            return EMPTY_BYTES;
        }
        byte[] buffer = new byte[len];
        readFully(buffer);
        return buffer;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
