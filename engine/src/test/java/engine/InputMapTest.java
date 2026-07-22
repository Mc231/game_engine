package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InputMapTest {

    @Test
    void bindReturnsSameInstance() {
        InputMap map = new InputMap();
        assertSame(map, map.bind("jump", 32));
    }

    @Test
    void keysReturnsBoundCodes() {
        InputMap map = new InputMap().bind("moveForward", 87, 265);
        assertArrayEquals(new int[]{87, 265}, map.keys("moveForward"));
    }

    @Test
    void unknownActionReturnsEmptyArray() {
        InputMap map = new InputMap();
        assertArrayEquals(new int[0], map.keys("nope"));
    }

    @Test
    void rebindingReplaces() {
        InputMap map = new InputMap()
                .bind("fire", 90)
                .bind("fire", 341, 342);
        assertArrayEquals(new int[]{341, 342}, map.keys("fire"));
    }
}
