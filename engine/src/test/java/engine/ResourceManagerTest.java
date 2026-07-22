package engine;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResourceManagerTest {

    /** A no-OpenGL {@link Disposable} that records how often it was disposed. */
    private static final class FakeResource implements Disposable {
        final int id;
        int disposeCount = 0;

        FakeResource(int id) {
            this.id = id;
        }

        @Override
        public void dispose() {
            disposeCount++;
        }
    }

    @Test
    void sameKeyLoadsOnceAndReturnsSameInstance() {
        ResourceManager rm = new ResourceManager();
        AtomicInteger loads = new AtomicInteger();

        FakeResource first = rm.get("k", () -> new FakeResource(loads.incrementAndGet()));
        FakeResource second = rm.get("k", () -> new FakeResource(loads.incrementAndGet()));

        assertSame(first, second);
        assertEquals(1, loads.get());
        assertEquals(1, rm.size());
    }

    @Test
    void differentKeysProduceDifferentInstances() {
        ResourceManager rm = new ResourceManager();

        FakeResource a = rm.get("a", () -> new FakeResource(1));
        FakeResource b = rm.get("b", () -> new FakeResource(2));

        assertNotSame(a, b);
        assertEquals(2, rm.size());
    }

    @Test
    void disposeReleasesEveryResourceOnceAndEmptiesCache() {
        ResourceManager rm = new ResourceManager();
        FakeResource a = rm.get("a", () -> new FakeResource(1));
        FakeResource b = rm.get("b", () -> new FakeResource(2));
        FakeResource c = rm.get("c", () -> new FakeResource(3));

        rm.dispose();

        assertEquals(1, a.disposeCount);
        assertEquals(1, b.disposeCount);
        assertEquals(1, c.disposeCount);
        assertEquals(0, rm.size());
    }

    @Test
    void doubleDisposeDoesNotReleaseTwice() {
        ResourceManager rm = new ResourceManager();
        FakeResource a = rm.get("a", () -> new FakeResource(1));

        rm.dispose();
        rm.dispose();

        assertEquals(1, a.disposeCount);
        assertEquals(0, rm.size());
    }
}
