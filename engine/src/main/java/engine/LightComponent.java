package engine;

import org.joml.Vector3f;

/**
 * A {@link Component} that carries a {@link Light} and keeps its position in sync
 * with the entity: each update copies the entity's world-space translation into
 * {@code light.position}, so moving the entity moves the light.
 */
public class LightComponent extends Component {

    private final Light light;

    public LightComponent(Light light) {
        this.light = light;
    }

    public Light light() {
        return light;
    }

    /** Copy the entity's world-space translation into the light's position. */
    @Override
    public void update(float dt) {
        entity().worldMatrix().getTranslation(light.position);
    }
}
