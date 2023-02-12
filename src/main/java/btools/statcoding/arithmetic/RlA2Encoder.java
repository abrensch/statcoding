package btools.statcoding.arithmetic;

import java.io.IOException;
import java.util.ArrayList;

import btools.statcoding.BitOutputStream;

public class RlA2Encoder {

    private long maxValue;
    private long minRunlength;
    private long lastValue;
    private long contextValue;
    private long repCount;
    private ACContextEncoder[] encoders;
    private int rleEscape = 0;
    private int pass;
    ArithmeticEncoder aEncoder;

    public RlA2Encoder(long maxValue, long minRunlength) {
        this.maxValue = maxValue;
        this.minRunlength = minRunlength;
        int n = (int) (maxValue + 2); // [0..maxValue,runLength]
        encoders = new ACContextEncoder[n];
        for (int i = 0; i < n; i++) {
            encoders[i] = new ACContextEncoder();
        }
    }

    public void init(BitOutputStream bos) throws IOException {
        if (++pass == 2) {
            bos.encodeUnsignedVarBits(maxValue, 0);
            bos.encodeUnsignedVarBits(minRunlength, 0);
            aEncoder = new ArithmeticEncoder(bos);
        }
        int n = encoders.length;
        for (int i = 0; i < n; i++) {
            encoders[i].init(aEncoder);
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
            flushLastValue();
        }
        lastValue = value;
        repCount++;
    }

    private void flushLastValue() throws IOException {
        if (repCount >= minRunlength) {
            encoders[(int) contextValue].write(rleEscape); // prefix runlength escape
            encoders[encoders.length - 1].write((int) repCount); // write runlength
            repCount = 1;
        }
        while (repCount > 0) {
            encoders[(int) contextValue].write((int) (lastValue + 1L));
            contextValue = lastValue;
            repCount--;
        }
    }

    public void finish() throws IOException {
        flushLastValue();
        if (aEncoder != null) {
            aEncoder.finish();
        }
    }
}
