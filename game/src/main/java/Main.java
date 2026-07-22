import engine.Application;
import scenes.CameraCubeScene;
import scenes.LightsScene;
import scenes.LitCubeScene;
import scenes.MaterialScene;
import scenes.ModelScene;
import scenes.ShadowScene;
import scenes.TerrainScene;
import scenes.TexturedCubeScene;
import scenes.TriangleScene;

/**
 * Entry point. Defines the whole game in one fluent block: window config +
 * the scenes to register. Switch scenes at runtime with the number keys
 * (1..9, then 0 for a 10th).
 */
public class Main {
    public static void main(String[] args) {
        Application.create()
                .title("LWJGL Engine")
                .size(1000, 700)
                .clearColor(0.05f, 0.05f, 0.07f, 1.0f)
                .scene(new TriangleScene())        // 1
                .scene(new TexturedCubeScene())    // 2
                .scene(new CameraCubeScene())      // 3
                .scene(new LitCubeScene())         // 4
                .scene(new MaterialScene())        // 5
                .scene(new LightsScene())          // 6
                .scene(new ModelScene())           // 7
                .scene(new ShadowScene())          // 8
                .scene(new TerrainScene())         // 9
                .run();
    }
}
