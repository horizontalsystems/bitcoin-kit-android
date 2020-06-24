package io.horizontalsystems.bitcoincore.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class BitcoinInputMarkable extends BitcoinInput {

    public int count;

    public BitcoinInputMarkable(byte[] data) {
        super(new ByteArrayInputStream(data));
        this.count = data.length;
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    public void reset() throws IOException {
        in.reset();
    }
}
