package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseTest {

    @Test
    void sameSeedIsDeterministic() {
        Noise a = new Noise(42);
        Noise b = new Noise(42);
        for (double x = -5; x <= 5; x += 0.7) {
            for (double y = -5; y <= 5; y += 0.7) {
                assertEquals(a.noise(x, y), b.noise(x, y), 0.0);
                assertEquals(a.fbm(x, y, 6, 2.0, 0.5), b.fbm(x, y, 6, 2.0, 0.5), 0.0);
            }
        }
    }

    @Test
    void differentSeedsDiffer() {
        Noise a = new Noise(1);
        Noise b = new Noise(2);
        boolean anyDifferent = false;
        for (double x = 0; x < 10 && !anyDifferent; x += 0.5) {
            if (a.noise(x, x * 1.3) != b.noise(x, x * 1.3)) {
                anyDifferent = true;
            }
        }
        assertTrue(anyDifferent, "different seeds should produce different noise");
    }

    @Test
    void outputStaysInReasonableRange() {
        Noise n = new Noise(7);
        for (double x = -50; x <= 50; x += 1.3) {
            for (double y = -50; y <= 50; y += 1.7) {
                double v = n.noise(x, y);
                assertTrue(v >= -1.5 && v <= 1.5, "noise out of range: " + v);
                double f = n.fbm(x, y, 6, 2.0, 0.5);
                assertTrue(f >= -1.2 && f <= 1.2, "fbm out of range: " + f);
            }
        }
    }

    @Test
    void producesVariation() {
        Noise n = new Noise(99);
        double first = n.noise(0.5, 0.5);
        boolean varied = false;
        for (double x = 0; x < 20 && !varied; x += 0.5) {
            if (Math.abs(n.noise(x, x) - first) > 0.1) {
                varied = true;
            }
        }
        assertTrue(varied, "noise should vary across the plane");
    }
}
