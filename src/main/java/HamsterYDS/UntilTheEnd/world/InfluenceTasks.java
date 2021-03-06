package HamsterYDS.UntilTheEnd.world;

import java.util.HashMap;
import java.util.UUID;

import HamsterYDS.UntilTheEnd.internal.LightingCompensation;
import HamsterYDS.UntilTheEnd.internal.NPCChecker;
import HamsterYDS.UntilTheEnd.internal.ResidenceChecker;
import HamsterYDS.UntilTheEnd.internal.UTEi18n;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import HamsterYDS.UntilTheEnd.Config;
import HamsterYDS.UntilTheEnd.UntilTheEnd;
import HamsterYDS.UntilTheEnd.event.hud.SanityChangeEvent;
import HamsterYDS.UntilTheEnd.event.hud.SanityChangeEvent.ChangeCause;
import HamsterYDS.UntilTheEnd.player.PlayerManager;
import HamsterYDS.UntilTheEnd.player.death.DeathCause;
import HamsterYDS.UntilTheEnd.player.death.DeathMessage;

/**
 * @author 鍗楀涓朵粨榧�
 * @version V5.1.1
 */
public class InfluenceTasks {
    public static UntilTheEnd plugin = UntilTheEnd.getInstance();
    public static int warn = plugin.getConfig().getInt("world.darkness.warnTime");
    public static int attack = plugin.getConfig().getInt("world.darkness.attackTime");
    public static int damage = plugin.getConfig().getInt("world.darkness.darkDamage");
    public static int san_warn = plugin.getConfig().getInt("world.darkness.sanWarn");
    public static int san_attack = plugin.getConfig().getInt("world.darkness.sanAttack");
    public static int carrotEffect = plugin.getConfig().getInt("world.darkness.carrotEffect");

    public static void initialize(UntilTheEnd plugin) {
        Darkness dark = new Darkness();
        dark.runTaskTimer(plugin, 0L, 20L);
        plugin.getServer().getPluginManager().registerEvents(dark, plugin);
    }

    public static class Darkness extends BukkitRunnable implements Listener {
        private final HashMap<UUID, Integer> darkness = new HashMap<>();
        private final HashMap<UUID, Integer> carrotEffects = new HashMap<>();

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onUse(PlayerItemConsumeEvent event) {
            Player player = event.getPlayer();
            if (event.getItem().getType() == Material.CARROT) {
                int currentEffect = 0;
                if (carrotEffects.containsKey(player.getUniqueId()))
                    currentEffect += carrotEffects.get(player.getUniqueId());
                carrotEffects.put(player.getUniqueId(), currentEffect + carrotEffect);
            }
            if (event.getItem().getType() == Material.GOLDEN_CARROT) {
                int currentEffect = 0;
                if (carrotEffects.containsKey(player.getUniqueId()))
                    currentEffect += carrotEffects.get(player.getUniqueId());
                carrotEffects.put(player.getUniqueId(), currentEffect + carrotEffect * 5);
            }
        }

        @EventHandler()
        public void onDeath(PlayerDeathEvent event) {
            if (darkness.containsKey(event.getEntity().getUniqueId())) {
                if (carrotEffects.containsKey(event.getEntity().getUniqueId())) {
                    int currentEffect = carrotEffects.get(event.getEntity().getUniqueId());
                    carrotEffects.remove(event.getEntity().getUniqueId());
                    carrotEffects.put(event.getEntity().getUniqueId(), 10 + currentEffect);
                } else carrotEffects.put(event.getEntity().getUniqueId(), 10);
                darkness.remove(event.getEntity().getUniqueId());
            }
        }

        @Override
        public void run() {
            for (World world : Config.enableWorlds) {
                for (Player player : world.getPlayers()) {
                    if (NPCChecker.isNPC(player) || ResidenceChecker.isProtected(player.getLocation())) continue;
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                        continue;
                    if (carrotEffects.containsKey(player.getUniqueId())) {
                        int currentEffect = carrotEffects.get(player.getUniqueId());
                        carrotEffects.put(player.getUniqueId(), currentEffect - 1);
                        if (currentEffect >= 0) continue;
                    }
                    if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) continue;
                    int needToCheck = 0; // TODO: Config setting.
                    final Location location = player.getLocation().add(0, 1, 0); // 鍗＄伒榄傛矙
                    if (location.getY() < 1) return;
                    final Block block = location.getBlock();
                    if (location.getY() >= location.getWorld().getHighestBlockYAt(location.getBlockX(), location.getBlockZ())) {
                        needToCheck -= LightingCompensation.getComp(player.getWorld().getUID());
                    }
                    if ((block.getLightFromBlocks() <= needToCheck) && (
                            block.getLightLevel() <= needToCheck
                    )) {
                        if (darkness.containsKey(player.getUniqueId())) {
                            int second = darkness.get(player.getUniqueId());
                            darkness.remove(player.getUniqueId());
                            darkness.put(player.getUniqueId(), second + 1);
                        } else darkness.put(player.getUniqueId(), 1);
                    } else darkness.remove(player.getUniqueId());

                    if (darkness.containsKey(player.getUniqueId())) {
                        if (darkness.get(player.getUniqueId()) == warn) {
                            player.sendTitle(UTEi18n.cache("mechanism.darkness.who-is-there.main"), UTEi18n.cache("mechanism.darkness.who-is-there.sub"));
                            SanityChangeEvent event = new SanityChangeEvent(player, ChangeCause.DARKWARN, san_warn);
                            Bukkit.getPluginManager().callEvent(event);
                            if (!event.isCancelled())
                                PlayerManager.change(player, PlayerManager.CheckType.SANITY, san_warn);
                        }
                        if (darkness.get(player.getUniqueId()) >= attack) {
                            player.sendTitle(UTEi18n.cache("mechanism.darkness.hurt-me.main"), UTEi18n.cache("mechanism.darkness.hurt-me.sub"));
                            player.damage(damage);
                            if (player.getHealth() <= san_attack)
                                DeathMessage.causes.put(player.getName(), DeathCause.DARKNESS);
                            SanityChangeEvent event = new SanityChangeEvent(player, ChangeCause.DARKATTACK, san_attack);
                            Bukkit.getPluginManager().callEvent(event);
                            if (!event.isCancelled())
                                PlayerManager.change(player, PlayerManager.CheckType.SANITY, san_attack);
                        }
                    }
                }
            }
        }
    }
}
