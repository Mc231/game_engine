package scenes.gta;

import engine.CarController;

/** Shared ground fields for the (currently flat) GTA city. Phase 2's city is flat. */
final class GtaGround {

    /** A flat ground at y = 0 for the driving physics. */
    static final CarController.Ground FLAT = (x, z) -> 0f;

    private GtaGround() {
    }
}
