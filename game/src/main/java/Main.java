import engine.Engine;
import engine.Scene;
import engine.WindowConfig;
import scenes.CameraCubeScene;
import scenes.LightsScene;
import scenes.LitCubeScene;
import scenes.MaterialScene;
import scenes.ModelScene;
import scenes.ShadowScene;
import scenes.TerrainScene;
import scenes.TexturedCubeScene;
import scenes.TriangleScene;

import java.util.List;

/**
 * Entry point. Configures a window and hands the Engine a list of scenes.
 * Switch scenes at runtime with the number keys (1..9, then 0 for the 10th).
 */
public class Main {
    public static void main(String[] args) {
        WindowConfig config = WindowConfig.builder()
                .size(1000, 700)
                .title("LWJGL Engine")
                .vsync(true)
                .clearColor(0.05f, 0.05f, 0.07f, 1.0f)
                .build();

        List<Scene> scenes = List.of(
                new TriangleScene(),        // 1
                new TexturedCubeScene(),    // 2
                new CameraCubeScene(),      // 3
                new LitCubeScene(),         // 4
                new MaterialScene(),        // 5
                new LightsScene(),          // 6
                new ModelScene(),           // 7
                new ShadowScene(),          // 8
                new TerrainScene()          // 9
        );

        new Engine(config, scenes).run();
    }
}
