package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * A compiled + linked vertex/fragment shader program. Construct it with GLSL
 * source; set uniforms by name (locations are looked up once and cached).
 */
public class ShaderProgram implements Disposable {

    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /** Build a program from two GLSL files on the classpath (e.g. "shaders/lit.vert"). */
    public static ShaderProgram fromFiles(String vertexResource, String fragmentResource) {
        return new ShaderProgram(readResource(vertexResource), readResource(fragmentResource));
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertex = compile(GL_VERTEX_SHADER, vertexSource);
        int fragment = compile(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program link failed:\n" + glGetProgramInfoLog(programId));
        }

        // Shaders are baked into the program now; the individual objects are free to go.
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile failed:\n" + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private int location(String name) {
        return uniformLocations.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }

    public void setUniform(String name, float value) {
        glUniform1f(location(name), value);
    }

    /** For int uniforms — notably a {@code sampler2D}, set to a texture-unit index. */
    public void setUniform(String name, int value) {
        glUniform1i(location(name), value);
    }

    public void setUniform(String name, Vector3f value) {
        glUniform3f(location(name), value.x, value.y, value.z);
    }

    public void setUniform(String name, Matrix4f value) {
        try (MemoryStack stack = stackPush()) {
            // JOML writes column-major, exactly what OpenGL wants → no transpose.
            glUniformMatrix4fv(location(name), false, value.get(stack.mallocFloat(16)));
        }
    }

    @Override
    public void dispose() {
        glDeleteProgram(programId);
    }
}
