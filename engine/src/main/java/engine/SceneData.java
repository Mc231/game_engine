package engine;

import java.util.List;

/**
 * The root of a JSON scene description: the list of entities that make up a level.
 */
public record SceneData(List<EntityData> entities) {
}
