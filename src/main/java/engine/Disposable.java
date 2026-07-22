package engine;

/**
 * Anything holding native GPU/GLFW resources that must be released explicitly.
 * OpenGL objects are not garbage-collected, so every wrapper implements this.
 */
public interface Disposable {
    void dispose();
}
