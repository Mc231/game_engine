package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure terrain height function (no Mesh/GL construction).
 */
class TerrainHeightTest {

    @Test
    void heightAlwaysWithinZeroAndMax() {
        Noise noise = new Noise(1337);
        float maxHeight = 70f;
        for (float x = -200; x <= 200; x += 11f) {
            for (float z = -200; z <= 200; z += 13f) {
                float h = Terrain.heightAt(noise, x, z, maxHeight);
                assertTrue(h >= 0f && h <= maxHeight, "height out of [0,max]: " + h);
            }
        }
    }

    @Test
    void heightIsDeterministic() {
        Noise noise = new Noise(1337);
        assertEquals(
                Terrain.heightAt(noise, 12.5f, -33.3f, 70f),
                Terrain.heightAt(noise, 12.5f, -33.3f, 70f),
                0.0);
    }

    @Test
    void heightVariesAcrossTerrain() {
        Noise noise = new Noise(1337);
        float base = Terrain.heightAt(noise, 0f, 0f, 70f);
        boolean varied = false;
        for (float x = 0; x < 300 && !varied; x += 7f) {
            if (Math.abs(Terrain.heightAt(noise, x, x, 70f) - base) > 1f) {
                varied = true;
            }
        }
        assertTrue(varied, "terrain should not be flat");
    }

    @Test
    void maxHeightScalesLinearly() {
        Noise noise = new Noise(5);
        float h1 = Terrain.heightAt(noise, 20f, 40f, 100f);
        float h2 = Terrain.heightAt(noise, 20f, 40f, 200f);
        assertEquals(2f * h1, h2, 1e-3f);
    }
}
