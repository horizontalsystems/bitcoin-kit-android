package io.horizontalsystems.bitcoincore.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A child class of BitcoinInput and extends its functionality.
 * @see BitcoinInput
 */
public final class BitcoinInputMarkable extends BitcoinInput {
    //Stores the length of the data byte array.
    public int count;
    /**
    @param data Byte array that is going to by read by the InputStream
    */
    public BitcoinInputMarkable(byte[] data) {
        super(new ByteArrayInputStream(data));
        this.count = data.length;
    }

    /**
     *Marks the InputStream
     */
    public void mark() {
        // since the readlimit for ByteArrayInputStream has no meaning set it to 0
        in.mark(0);
    }
    /**
     *Resets the InputStream
     */
    public void reset() throws IOException {
        in.reset();
    }
}
