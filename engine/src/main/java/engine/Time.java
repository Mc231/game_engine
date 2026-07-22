package engine;

/**
 * Frame-timing helper. Tracks the per-frame delta, total elapsed time, a running
 * frame count, and a smoothed FPS over a rolling ~1-second window.
 *
 * <p>Pure and testable: {@link #update(double)} takes the current time as a
 * parameter, so it never touches GLFW or the GL clock. Feed it
 * {@code glfwGetTime()} from the game loop (or synthetic values from a test).
 */
public class Time {

    private boolean started = false;
    private double lastCurrent = 0.0;   // time passed to the previous update()

    private float deltaSeconds = 0f;
    private double totalSeconds = 0.0;
    private long frameCount = 0L;

    // Rolling FPS window.
    private int fps = 0;
    private int framesThisWindow = 0;
    private double windowStart = 0.0;

    /**
     * Advance the clock to {@code currentTimeSeconds}. The first call establishes
     * the baseline (delta stays 0); every later call computes the delta from the
     * previous call, accumulates the total, and refreshes the FPS roughly once a
     * second.
     */
    public void update(double currentTimeSeconds) {
        if (!started) {
            started = true;
            lastCurrent = currentTimeSeconds;
            windowStart = currentTimeSeconds;
            deltaSeconds = 0f;
            return;
        }

        deltaSeconds = (float) (currentTimeSeconds - lastCurrent);
        lastCurrent = currentTimeSeconds;
        totalSeconds += deltaSeconds;
        frameCount++;

        // Recompute FPS over the last ~1 second, then reset the window.
        framesThisWindow++;
        double windowElapsed = currentTimeSeconds - windowStart;
        if (windowElapsed >= 1.0) {
            fps = (int) Math.round(framesThisWindow / windowElapsed);
            framesThisWindow = 0;
            windowStart = currentTimeSeconds;
        }
    }

    /** Seconds elapsed since the previous frame (0 on the very first update). */
    public float deltaSeconds() {
        return deltaSeconds;
    }

    /** Total seconds accumulated across all frames since the baseline. */
    public double totalSeconds() {
        return totalSeconds;
    }

    /** Number of frames counted (updates after the baseline). */
    public long frameCount() {
        return frameCount;
    }

    /** Smoothed frames-per-second from the most recent 1-second window. */
    public int fps() {
        return fps;
    }
}
