package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcsTest {

    private static final float EPS = 1e-5f;

    /** A component that counts how many times it was updated. */
    static class Counter extends Component {
        int ticks = 0;

        @Override
        public void update(float dt) {
            ticks++;
        }
    }

    /** A trivial marker component for collection tests. */
    static class Marker extends Component {
    }

    @Test
    void getReturnsAttachedComponent() {
        Entity e = new Entity("e");
        Counter c = new Counter();
        e.add(c);
        assertSame(c, e.get(Counter.class));
    }

    @Test
    void getReturnsNullForAbsentType() {
        Entity e = new Entity("e");
        e.add(new Counter());
        assertNull(e.get(Marker.class));
    }

    @Test
    void worldMatrixComposesParentAndChild() {
        Entity parent = new Entity("parent");
        parent.transform().setPosition(10, 0, 0);
        Entity child = new Entity("child");
        child.transform().setPosition(1, 0, 0);
        parent.addChild(child);

        Vector3f t = child.worldMatrix().getTranslation(new Vector3f());
        assertVec(11, 0, 0, t);
    }

    @Test
    void worldMatrixOfRootIsLocal() {
        Entity root = new Entity("root");
        root.transform().setPosition(3, 4, 5);
        Vector3f t = root.worldMatrix().getTranslation(new Vector3f());
        assertVec(3, 4, 5, t);
    }

    @Test
    void worldUpdatePropagatesToParentAndChild() {
        Entity parent = new Entity("parent");
        Counter pc = new Counter();
        parent.add(pc);
        Entity child = new Entity("child");
        Counter cc = new Counter();
        child.add(cc);
        parent.addChild(child);

        World world = new World();
        world.add(parent);
        world.update(0.016f);

        assertEquals(1, pc.ticks);
        assertEquals(1, cc.ticks);
    }

    @Test
    void collectGathersComponentsAcrossHierarchy() {
        Entity parent = new Entity("parent");
        Marker pm = new Marker();
        parent.add(pm);
        Entity child = new Entity("child");
        Marker cm = new Marker();
        child.add(cm);
        parent.addChild(child);

        World world = new World();
        world.add(parent);

        var found = world.collect(Marker.class);
        assertEquals(2, found.size());
        assertTrue(found.contains(pm));
        assertTrue(found.contains(cm));
    }

    private static void assertVec(float x, float y, float z, Vector3f v) {
        assertEquals(x, v.x, EPS);
        assertEquals(y, v.y, EPS);
        assertEquals(z, v.z, EPS);
    }
}
