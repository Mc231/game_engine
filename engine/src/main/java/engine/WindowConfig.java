package engine;

/**
 * Immutable window/OpenGL configuration, built with a fluent builder.
 *
 * <pre>{@code
 * WindowConfig cfg = WindowConfig.builder()
 *         .size(1280, 720)
 *         .title("My App")
 *         .vsync(true)
 *         .clearColor(0.1f, 0.12f, 0.15f, 1f)
 *         .build();
 * }</pre>
 */
public final class WindowConfig {

    public final int width;
    public final int height;
    public final String title;
    public final boolean resizable;
    public final boolean vsync;
    public final int glMajor;
    public final int glMinor;
    public final float clearR, clearG, clearB, clearA;

    private WindowConfig(Builder b) {
        this.width = b.width;
        this.height = b.height;
        this.title = b.title;
        this.resizable = b.resizable;
        this.vsync = b.vsync;
        this.glMajor = b.glMajor;
        this.glMinor = b.glMinor;
        this.clearR = b.clearR;
        this.clearG = b.clearG;
        this.clearB = b.clearB;
        this.clearA = b.clearA;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder holding the defaults; every setter returns {@code this}. */
    public static final class Builder {
        private int width = 800;
        private int height = 600;
        private String title = "LWJGL";
        private boolean resizable = true;
        private boolean vsync = true;
        private int glMajor = 3;
        private int glMinor = 3;
        private float clearR = 0.1f, clearG = 0.12f, clearB = 0.15f, clearA = 1f;

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        public Builder vsync(boolean vsync) {
            this.vsync = vsync;
            return this;
        }

        /** OpenGL context version to request (Core profile is always used). */
        public Builder glVersion(int major, int minor) {
            this.glMajor = major;
            this.glMinor = minor;
            return this;
        }

        public Builder clearColor(float r, float g, float b, float a) {
            this.clearR = r;
            this.clearG = g;
            this.clearB = b;
            this.clearA = a;
            return this;
        }

        public WindowConfig build() {
            return new WindowConfig(this);
        }
    }
}
