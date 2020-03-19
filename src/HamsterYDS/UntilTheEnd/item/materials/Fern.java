package HamsterYDS.UntilTheEnd.item.materials;

import HamsterYDS.UntilTheEnd.internal.EventHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import HamsterYDS.UntilTheEnd.item.ItemManager;

/**
 * @author 南外丶仓鼠
 * @version V5.1.1
 */
public class Fern implements Listener {
    public static double heal = ItemManager.itemAttributes.getDouble("Fern.heal");

    public Fern() {
        ItemManager.plugin.getServer().getPluginManager().registerEvents(this, ItemManager.plugin);
    }

    @EventHandler
    public void onRight(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!EventHelper.isRight(event.getAction())) return;
        ItemStack item = event.getItem();
        if (ItemManager.isSimilar(item, getClass())) {
            if (player.getHealth() + heal >= player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            else player.setHealth(player.getHealth() + heal);
        }
    }
}
