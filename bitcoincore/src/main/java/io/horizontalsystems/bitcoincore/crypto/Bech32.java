package io.horizontalsystems.bitcoincore.crypto;

import androidx.annotation.NonNull;

public class Bech32 {
    /** The Bech32 character set for encoding. */
    static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    /** The Bech32 character set for decoding. */
    static final byte[] CHARSET_REV = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            15, -1, 10, 17, 21, 20, 26, 30,  7,  5, -1, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
            1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
            1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1
    };

    public static class Bech32Data {
        public final String hrp;
        public final byte[] data;
        @NonNull
        public final Encoding encoding;

        Bech32Data(final String hrp, final byte[] data) {
            this.hrp = hrp;
            this.data = data;
            this.encoding = (data[0] == 0x00 ? Encoding.BECH32 : Encoding.BECH32M);
        }

        public Bech32Data(String hrp, byte[] data, Encoding encoding) {
            this.hrp = hrp;
            this.data = data;
            this.encoding = encoding;
        }
    }

    public enum Encoding {
        BECH32(1), BECH32M(0x2bc830a3);

        final int checksumConstant;

        Encoding(int checksumConstant) {
            this.checksumConstant = checksumConstant;
        }
    }
}
