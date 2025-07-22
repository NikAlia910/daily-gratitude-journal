package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class GratitudeEntryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static GratitudeEntry getGratitudeEntrySample1() {
        return new GratitudeEntry().id(1L);
    }

    public static GratitudeEntry getGratitudeEntrySample2() {
        return new GratitudeEntry().id(2L);
    }

    public static GratitudeEntry getGratitudeEntryRandomSampleGenerator() {
        return new GratitudeEntry().id(longCount.incrementAndGet());
    }
}
