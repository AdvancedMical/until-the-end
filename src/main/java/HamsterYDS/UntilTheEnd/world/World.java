package HamsterYDS.UntilTheEnd.world;

import HamsterYDS.UntilTheEnd.Logging;
import HamsterYDS.UntilTheEnd.UntilTheEnd;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;
//import HamsterYDS.UntilTheEnd.world.nms.DarkNight;

/**
 * @author 南外丶仓鼠
 * @version V5.1.1
 */
public class World {
    public static File file;
    public static YamlConfiguration yaml;

    public static void initialize(UntilTheEnd plugin) {
        file = new File(plugin.getDataFolder(), "worlds.yml");
        yaml = YamlConfiguration.loadConfiguration(file);
        Logging.getLogger().log(Level.FINER, () -> "[World] Loading world data.");
        Logging.getLogger().log(Level.FINER, () -> "[World] File: " + file);
        Logging.getLogger().log(Level.FINER, () -> "[World] Yaml: " + yaml);
        assert yaml != null : "Error: Yaml not loaded.";
        WorldProvider.loadWorlds();
        WorldProvider.registerListener();
        new WorldCounter().runTaskTimer(plugin, 0L, 20L);
        InfluenceTasks.initialize(plugin);
//		new DarkNight().runTaskTimer(plugin,0L,50L);
    }
}
