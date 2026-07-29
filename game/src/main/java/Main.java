import engine.Application;
import engine.Settings;
import scenes.gta.GtaScene;

/**
 * Entry point for <b>Grand Theft LWJGL</b> (codename MiniCity) — a small
 * third-person open-world game built on the engine. Loads {@link Settings} from a
 * file, then configures the window and runs the game scene ({@link GtaScene},
 * which lives in {@code scenes.gta}; see {@code docs/gta/}).
 */
public class Main {
    public static void main(String[] args) {
        Settings settings = Settings.load("settings.properties");

        Application.create()
                .title("Grand Theft LWJGL")
                .size(settings.width, settings.height)
                .vsync(settings.vsync)
                .clearColor(0.05f, 0.05f, 0.07f, 1.0f)
                .scene(new GtaScene())
                .run();
    }
}
