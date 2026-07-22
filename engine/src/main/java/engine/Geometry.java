package engine;

/**
 * Static factory for common vertex data. Keeps big vertex arrays out of scenes.
 */
public final class Geometry {

    private Geometry() {
    }

    /**
     * A unit cube (−0.5..0.5) as 36 vertices, each: position(3) + normal(3) +
     * uv(2). Use with a {@code new int[]{3, 3, 2}} attribute layout. Each face
     * has one outward normal and maps the full texture.
     */
    /**
     * A flat square on the XZ plane (y = 0), from -half..half, facing up.
     * Layout: position(3) + normal(3) + uv(2). {@code uvTiles} repeats the
     * texture across the surface.
     */
    public static float[] plane(float half, float uvTiles) {
        float t = uvTiles;
        return new float[]{
                // position                 normal          uv
                -half, 0f, -half,   0f, 1f, 0f,   0f, 0f,
                -half, 0f,  half,   0f, 1f, 0f,   0f, t,
                 half, 0f,  half,   0f, 1f, 0f,   t,  t,
                -half, 0f, -half,   0f, 1f, 0f,   0f, 0f,
                 half, 0f,  half,   0f, 1f, 0f,   t,  t,
                 half, 0f, -half,   0f, 1f, 0f,   t,  0f,
        };
    }

    public static float[] cubeWithNormalsAndUV() {
        return new float[]{
                // front  (normal 0,0,1)
                -0.5f,-0.5f, 0.5f,  0f,0f,1f,  0f,0f,   0.5f,-0.5f, 0.5f,  0f,0f,1f,  1f,0f,   0.5f, 0.5f, 0.5f,  0f,0f,1f,  1f,1f,
                -0.5f,-0.5f, 0.5f,  0f,0f,1f,  0f,0f,   0.5f, 0.5f, 0.5f,  0f,0f,1f,  1f,1f,  -0.5f, 0.5f, 0.5f,  0f,0f,1f,  0f,1f,
                // back   (normal 0,0,-1)
                -0.5f,-0.5f,-0.5f,  0f,0f,-1f, 0f,0f,  -0.5f, 0.5f,-0.5f,  0f,0f,-1f, 0f,1f,   0.5f, 0.5f,-0.5f,  0f,0f,-1f, 1f,1f,
                -0.5f,-0.5f,-0.5f,  0f,0f,-1f, 0f,0f,   0.5f, 0.5f,-0.5f,  0f,0f,-1f, 1f,1f,   0.5f,-0.5f,-0.5f,  0f,0f,-1f, 1f,0f,
                // left   (normal -1,0,0)
                -0.5f, 0.5f, 0.5f, -1f,0f,0f,  1f,1f,  -0.5f, 0.5f,-0.5f, -1f,0f,0f,  0f,1f,  -0.5f,-0.5f,-0.5f, -1f,0f,0f,  0f,0f,
                -0.5f, 0.5f, 0.5f, -1f,0f,0f,  1f,1f,  -0.5f,-0.5f,-0.5f, -1f,0f,0f,  0f,0f,  -0.5f,-0.5f, 0.5f, -1f,0f,0f,  1f,0f,
                // right  (normal 1,0,0)
                 0.5f, 0.5f, 0.5f,  1f,0f,0f,  1f,1f,   0.5f,-0.5f,-0.5f,  1f,0f,0f,  0f,0f,   0.5f, 0.5f,-0.5f,  1f,0f,0f,  0f,1f,
                 0.5f, 0.5f, 0.5f,  1f,0f,0f,  1f,1f,   0.5f,-0.5f, 0.5f,  1f,0f,0f,  1f,0f,   0.5f,-0.5f,-0.5f,  1f,0f,0f,  0f,0f,
                // top    (normal 0,1,0)
                -0.5f, 0.5f,-0.5f,  0f,1f,0f,  0f,1f,  -0.5f, 0.5f, 0.5f,  0f,1f,0f,  0f,0f,   0.5f, 0.5f, 0.5f,  0f,1f,0f,  1f,0f,
                -0.5f, 0.5f,-0.5f,  0f,1f,0f,  0f,1f,   0.5f, 0.5f, 0.5f,  0f,1f,0f,  1f,0f,   0.5f, 0.5f,-0.5f,  0f,1f,0f,  1f,1f,
                // bottom (normal 0,-1,0)
                -0.5f,-0.5f,-0.5f,  0f,-1f,0f, 0f,1f,   0.5f,-0.5f,-0.5f,  0f,-1f,0f, 1f,1f,   0.5f,-0.5f, 0.5f,  0f,-1f,0f, 1f,0f,
                -0.5f,-0.5f,-0.5f,  0f,-1f,0f, 0f,1f,   0.5f,-0.5f, 0.5f,  0f,-1f,0f, 1f,0f,  -0.5f,-0.5f, 0.5f,  0f,-1f,0f, 0f,0f,
        };
    }
}
