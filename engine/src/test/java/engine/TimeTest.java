package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeTest {

    @Test
    void firstUpdateHasZeroDelta() {
        Time t = new Time();
        t.update(10.0);
        assertEquals(0f, t.deltaSeconds(), 0f);
        assertEquals(0.0, t.totalSeconds(), 0.0);
        assertEquals(0L, t.frameCount());
    }

    @Test
    void deltaIsComputedFromPreviousCall() {
        Time t = new Time();
        t.update(10.0);
        t.update(10.5);
        assertEquals(0.5f, t.deltaSeconds(), 1e-6f);
        t.update(10.75);
        assertEquals(0.25f, t.deltaSeconds(), 1e-6f);
    }

    @Test
    void totalSecondsAccumulates() {
        Time t = new Time();
        t.update(0.0);
        t.update(1.0);
        t.update(2.5);
        assertEquals(2.5, t.totalSeconds(), 1e-6);
    }

    @Test
    void frameCountIncrementsPerUpdate() {
        Time t = new Time();
        t.update(0.0);   // baseline, no frame counted
        assertEquals(0L, t.frameCount());
        t.update(0.1);
        t.update(0.2);
        t.update(0.3);
        assertEquals(3L, t.frameCount());
    }

    @Test
    void fpsIsApproximatelySixtyOverOneSecond() {
        Time t = new Time();
        // Baseline plus 60 evenly-spaced frames spanning exactly one second.
        for (int i = 0; i <= 60; i++) {
            t.update(i / 60.0);
        }
        assertEquals(60, t.fps(), 1);
    }
}
