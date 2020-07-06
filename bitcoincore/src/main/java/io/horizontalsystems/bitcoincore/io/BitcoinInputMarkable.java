package io.horizontalsystems.bitcoincore.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class BitcoinInputMarkable extends BitcoinInput {

    public int count;

    public BitcoinInputMarkable(byte[] data) {
        super(new ByteArrayInputStream(data));
        this.count = data.length;
    }

    public void mark() {
        // since the readlimit for ByteArrayInputStream has no meaning set it to 0
        in.mark(0);
    }

    public void reset() throws IOException {
        in.reset();
    }
}
