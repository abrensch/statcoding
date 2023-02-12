package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitInputStream;

public final class DiffInputCoder {

    private long lastValue;
    private long repCount;

    private LongDecoder diffDecoder = new SignedLongDecoder();
    private LongDecoder repDecoder = new LongDecoder();

    public void init(BitInputStream bis) throws IOException {
        diffDecoder.init(bis);
        repDecoder.init(bis);
    }

    public long readDiffed() throws IOException {
        if (repCount == 0) {
            lastValue += diffDecoder.decodeLong();
            repCount = repDecoder.decodeLong() + 1L;
        }
        repCount--;
        return lastValue;
    }

    private static class LongDecoder extends HuffmanDecoder {
        public long decodeLong() throws IOException {
            return ((Long) decodeObject()).longValue();
        }

        @Override
        protected Object decodeObjectFromStream() throws IOException {
            long lv = bis.decodeUnsignedVarBits(0);
            return Long.valueOf(lv);
        }
    }

    private static class SignedLongDecoder extends LongDecoder {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
            long lv = bis.decodeSignedVarBits(0);
            return Long.valueOf(lv);
        }
    }

}
