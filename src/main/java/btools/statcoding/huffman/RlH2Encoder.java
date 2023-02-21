package btools.statcoding.huffman;

import java.io.IOException;

import btools.statcoding.BitOutputStream;

public class RlH2Encoder {

    private final long maxValue;
    private final long minRunLength;
    private long lastValue;
    private long contextValue;
    private long repCount;
    private final HuffmanEncoder[] encoders;
    private static final Long rleEscape = -1L;
    private int pass;
    private BitOutputStream bos;

    public RlH2Encoder(long maxValue, long minRunLength) {
        this.maxValue = maxValue;
        this.minRunLength = minRunLength;
        encoders = new HuffmanEncoder[(int) (maxValue + 1)];
        int n = encoders.length;
        for (int i = 0; i < n; i++) {
            encoders[i] = new HuffmanEncoder() {
                @Override
                protected void encodeObjectToStream(Object obj) throws IOException {
                    bos.encodeBounded(maxValue + 1L, 1L + (Long) obj);
                }
            };
        }
    }

    public void init(BitOutputStream bos) throws IOException {
        if (++pass == 2) {
            bos.encodeUnsignedVarBits(maxValue, 0);
            bos.encodeUnsignedVarBits(minRunLength, 0);
        }
        this.bos = bos;
        for (HuffmanEncoder encoder: encoders ) {
            encoder.init(bos);
        }
        repCount = 0;
        lastValue = 0L;
        contextValue = 0L;
    }

    public void encodeValue(long value) throws IOException {

        if (value < 0L || value > maxValue) {
            throw new IllegalArgumentException("invalid value: " + value + " (maxValue=" + maxValue + ")");
        }
        if (value != lastValue) {
            if (repCount >= minRunLength) {
                encoders[(int) contextValue].encodeObject(rleEscape); // prefix run-length escape
                if (pass == 2) {
                    bos.encodeUnsignedVarBits(repCount - minRunLength, 0);
                }
                repCount = 1;
            }
            while (repCount > 0) {
                encoders[(int) contextValue].encodeObject(lastValue);
                contextValue = lastValue;
                repCount--;
            }
        }
        lastValue = value;
        repCount++;
    }

    public void finish() throws IOException {
        encodeValue((lastValue + 1) % (maxValue + 1));
    }
}
