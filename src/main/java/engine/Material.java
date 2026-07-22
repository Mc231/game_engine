package engine;

import org.joml.Vector3f;

/**
 * The "look" of a surface: a shader, an optional texture, and the parameters
 * the shader reads (tint, ambient/specular strength, shininess). Many materials
 * can share one shader and differ only in parameters, so several objects with
 * distinct looks still use a single GLSL program.
 *
 * {@link #use()} binds everything; the shader must declare matching uniforms
 * ({@code uTexture, uTint, uAmbientStrength, uSpecularStrength, uShininess}).
 * Uniforms a given shader doesn't declare are simply ignored.
 */
public class Material {

    private final ShaderProgram shader;
    private final Texture diffuse;               // may be null

    private final Vector3f tint = new Vector3f(1f, 1f, 1f);
    private float ambientStrength = 0.15f;
    private float specularStrength = 0.5f;
    private float shininess = 32f;

    public Material(ShaderProgram shader) {
        this(shader, null);
    }

    public Material(ShaderProgram shader, Texture diffuse) {
        this.shader = shader;
        this.diffuse = diffuse;
    }

    public Material setTint(float r, float g, float b) {
        tint.set(r, g, b);
        return this;
    }

    public Material setAmbientStrength(float v) {
        ambientStrength = v;
        return this;
    }

    public Material setSpecularStrength(float v) {
        specularStrength = v;
        return this;
    }

    public Material setShininess(float v) {
        shininess = v;
        return this;
    }

    public ShaderProgram shader() {
        return shader;
    }

    /** Bind the shader + texture and upload this material's parameters. */
    public void use() {
        shader.bind();
        if (diffuse != null) {
            diffuse.bind(0);
            shader.setUniform("uTexture", 0);
        }
        shader.setUniform("uTint", tint);
        shader.setUniform("uAmbientStrength", ambientStrength);
        shader.setUniform("uSpecularStrength", specularStrength);
        shader.setUniform("uShininess", shininess);
    }
}
