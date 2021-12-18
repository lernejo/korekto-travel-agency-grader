package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;

import java.util.UUID;

@SubjectForToolkitInclusion
public interface RandomSupplier {

    int nextInt(int bound);

    default boolean nextBoolean() {
        return nextInt(2) > 0;
    }

    default void nextBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = Integer.valueOf(nextInt(256) - 128).byteValue();
        }
    }

    default UUID nextUuid() {
        byte[] data = new byte[16];
        nextBytes(data);
        data[6] &= 0x0f;
        data[6] |= 0x40;
        data[8] &= 0x3f;
        data[8] |= 0x80;

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);
        long mostSigBits = msb;
        long leastSigBits = lsb;

        return new UUID(mostSigBits, leastSigBits);
    }
}
