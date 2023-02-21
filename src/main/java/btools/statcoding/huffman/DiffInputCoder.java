package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitInputStream;

public class DiffInputCoder {

    private long lastValue;
    private long repCount;

    private final LongDecoder diffDecoder = new SignedLongDecoder();
    private final LongDecoder repDecoder = new LongDecoder();

    public void init(BitInputStream bis) throws IOException {
        diffDecoder.init(bis);
        repDecoder.init(bis);
    }

    public long readDiffed() throws IOException {
        if (repCount == 0) {
            lastValue += (Long) diffDecoder.decodeObject();
            repCount = 1L + (Long) repDecoder.decodeObject();
        }
        repCount--;
        return lastValue;
    }

    private static class LongDecoder extends HuffmanDecoder {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
            return bis.decodeUnsignedVarBits(0);
        }
    }

    private static class SignedLongDecoder extends LongDecoder {
        @Override
        protected Object decodeObjectFromStream() throws IOException {
            return bis.decodeSignedVarBits(0);
        }
    }
}
