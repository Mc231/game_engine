import engine.Application;
import engine.Settings;
import scenes.CameraCubeScene;
import scenes.EcsScene;
import scenes.LightsScene;
import scenes.LitCubeScene;
import scenes.MaterialScene;
import scenes.ModelScene;
import scenes.NormalMapScene;
import scenes.PhysicsScene;
import scenes.SerializedScene;
import scenes.ShadowScene;
import scenes.SkyboxScene;
import scenes.TerrainScene;
import scenes.TexturedCubeScene;
import scenes.TriangleScene;
import scenes.WalkScene;

/**
 * Entry point. Loads {@link Settings} from a file, then defines the whole game
 * in one fluent block: window config + the scenes to register. Switch scenes at
 * runtime with the number keys (1..9, 0 for the 10th) or cycle with [ / ].
 */
public class Main {
    public static void main(String[] args) {
        Settings settings = Settings.load("settings.properties");

        Application.create()
                .title("LWJGL Engine")
                .size(settings.width, settings.height)
                .vsync(settings.vsync)
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
                .scene(new EcsScene())             // 0 (10th)
                .scene(new WalkScene())            // 11th ([ / ] to reach)
                .scene(new SkyboxScene())          // 12th ([ / ] to reach)
                .scene(new SerializedScene())      // 13th ([ / ] to reach)
                .scene(new NormalMapScene())       // 14th ([ / ] to reach)
                .scene(new PhysicsScene())         // 15th ([ / ] to reach)
                .run();
    }
}
