package engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * A caching, owning registry for GPU resources. Each distinct key is loaded at
 * most once and its result is cached; repeated lookups return the same instance.
 * <p>
 * The manager <em>owns</em> every resource it caches: {@link #dispose()} disposes
 * them all (in reverse insertion order) and clears the cache. Callers must never
 * dispose a resource obtained from this manager themselves.
 */
public class ResourceManager implements Disposable {

    // Insertion order matters: dispose() releases resources in reverse order.
    private final LinkedHashMap<String, Disposable> cache = new LinkedHashMap<>();

    /**
     * Return the resource cached under {@code key}, loading it via {@code loader}
     * on first access. The loader is invoked at most once per key.
     *
     * @param key    unique cache key
     * @param loader factory used only on a cache miss
     * @return the cached (or freshly loaded) resource
     */
    @SuppressWarnings("unchecked")
    public <T extends Disposable> T get(String key, Supplier<T> loader) {
        Disposable existing = cache.get(key);
        if (existing != null) {
            return (T) existing;
        }
        T loaded = loader.get();
        cache.put(key, loaded);
        return loaded;
    }

    /** Load-or-return a texture from a classpath image path. */
    public Texture texture(String path) {
        return get("texture:" + path, () -> new Texture(path));
    }

    /** Load-or-return a shader program from its vertex/fragment sources. */
    public ShaderProgram shader(String vert, String frag) {
        return get("shader:" + vert + "|" + frag, () -> ShaderProgram.fromFiles(vert, frag));
    }

    /** Load-or-return a mesh from a classpath OBJ path. */
    public Mesh mesh(String objPath) {
        return get("mesh:" + objPath, () -> OBJLoader.load(objPath));
    }

    /** Number of resources currently cached. */
    public int size() {
        return cache.size();
    }

    @Override
    public void dispose() {
        // Reverse insertion order so dependents are released before dependencies.
        List<Disposable> resources = new ArrayList<>(cache.values());
        for (int i = resources.size() - 1; i >= 0; i--) {
            resources.get(i).dispose();
        }
        // Clearing guards against a second dispose() releasing anything twice.
        cache.clear();
    }
}
