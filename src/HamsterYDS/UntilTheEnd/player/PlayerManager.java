package HamsterYDS.UntilTheEnd.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.*; 
import java.util.logging.Level;

import HamsterYDS.UntilTheEnd.Config;
import HamsterYDS.UntilTheEnd.internal.UTEi18n;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import HamsterYDS.UntilTheEnd.UntilTheEnd;
import HamsterYDS.UntilTheEnd.cap.HudProvider;
import HamsterYDS.UntilTheEnd.internal.pdl.PlayerDataLoaderImpl;
import HamsterYDS.UntilTheEnd.player.role.IRole;
import HamsterYDS.UntilTheEnd.player.role.Roles;

/**
 * @author 南外丶仓鼠
 * @version V5.1.1
 */
public class PlayerManager implements Listener {
    public static UntilTheEnd plugin = UntilTheEnd.getInstance();
    private static HashMap<UUID, IPlayer> players = new HashMap<>();
    public static final File playerdata = new File(plugin.getDataFolder(), "playerdata");

    public PlayerManager() {
    }

    public PlayerManager(UntilTheEnd plugin) {
        new SavingTask();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers())
            load(player);
    }

    public static ArrayList<UUID> playerChangedRole = new ArrayList<UUID>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        load(player);
        if (checkRole(player) == Roles.DEFAULT)
            player.sendMessage(UTEi18n.cache("role.unchosen"));
        HudProvider.sanity.put(name, " ");
        HudProvider.humidity.put(name, " ");
        HudProvider.temperature.put(name, " ");
        HudProvider.tiredness.put(name, " ");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        save(player);
        players.remove(player.getUniqueId());
        HudProvider.sanity.remove(name);
        HudProvider.humidity.remove(name);
        HudProvider.temperature.remove(name);
        HudProvider.tiredness.remove(name);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        IPlayer ip = players.get(event.getEntity().getUniqueId());
        if (ip == null) return;
        Player p = event.getEntity();
        forgetChange(event.getEntity(), CheckType.TEMPERATURE, getDefault(ip, CheckType.TEMPERATURE, p), EditAction.SET);
        forgetChange(event.getEntity(), CheckType.HUMIDITY, getDefault(ip, CheckType.HUMIDITY, p), EditAction.SET);
        forgetChange(event.getEntity(), CheckType.SANITY, getDefault(ip, CheckType.SANITY, p), EditAction.SET);
        forgetChange(event.getEntity(), CheckType.TIREDNESS, getDefault(ip, CheckType.TIREDNESS, p), EditAction.SET);
        changeRole(event.getEntity(), Roles.DEFAULT);
        save(event.getEntity());
        // load(event.getEntity());
    }

    public static void load(OfflinePlayer name) {
        double humidity = 0;
        double temperature = 37;
        double sanity = 200;
        double tiredness = 0;
        String roleName = "DEFAULT";
        int level = 0;
        int sanMax = 200;
        int healthMax = 20;
        double damageLevel = 1;

        try {
            final Map<String, Object> load = PlayerDataLoaderImpl.loader.load(playerdata, name);
            if (load != null) {
                humidity = ((Number) load.getOrDefault("humidity", 0)).doubleValue();
                temperature = ((Number) load.getOrDefault("temperature", 37)).doubleValue();
                sanity = ((Number) load.getOrDefault("sanity", 200)).doubleValue();
                tiredness = ((Number) load.getOrDefault("tiredness", 0)).doubleValue();
                roleName = ((String) load.getOrDefault("role", "DEFAULT"));
                Roles role = Roles.valueOf(roleName);
                level = ((Number) load.getOrDefault("level", role.originLevel)).intValue();
                sanMax = ((Number) load.getOrDefault("sanMax", role.originSanMax)).intValue();
                healthMax = ((Number) load.getOrDefault("healthMax", role.originHealthMax)).intValue();
                damageLevel = ((Number) load.getOrDefault("damageLevel", role.originDamageLevel)).intValue();
            }
        } catch (Throwable exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load " + name, exception);
        }
        IPlayer player = new IPlayer(temperature, humidity, sanity, tiredness);

        player.role = Roles.valueOf(roleName);
        player.roleStats = new IRole(level, sanMax, healthMax, damageLevel);

        players.put(name.getUniqueId(), player);
    }

    public static void save(OfflinePlayer name) {
        Map<String, Object> data = new HashMap<>();
        IPlayer player = players.get(name.getUniqueId());
        data.put("humidity", player.humidity);
        data.put("temperature", player.temperature);
        data.put("sanity", player.sanity);
        data.put("tiredness", player.tiredness);
        data.put("role", player.role.toString());
        data.put("level", player.roleStats.level);
        data.put("sanMax", player.roleStats.sanMax);
        data.put("healthMax", player.roleStats.healthMax);
        data.put("damageLevel", player.roleStats.damageLevel);
        try {
            PlayerDataLoaderImpl.loader.save(playerdata, name, data);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed save data for " + name, e);
        }
    }

    public static Roles checkRole(Player player) {
        if (!players.containsKey(player.getUniqueId())) return Roles.DEFAULT;
        IPlayer ip = players.get(player.getUniqueId());
        return ip.role == null ? Roles.DEFAULT : ip.role;
    }

    public static void changeRole(Player player, Roles newRole) {
        IPlayer ip = players.get(player.getUniqueId());
        ip.role = newRole;
        ip.roleStats = new IRole(newRole.originLevel, newRole.originSanMax,
                newRole.originHealthMax, newRole.originDamageLevel);
        players.remove(player.getUniqueId());
        players.put(player.getUniqueId(), ip);
        save(player);
        player.setMaxHealth(ip.roleStats.healthMax);
    }

    static double getDefault(IPlayer ip, CheckType type, Player player) {
        switch (type) {
            case TEMPERATURE:
                return 37;
            case HUMIDITY:
                return 0;
            case SANITY:
                return ip.roleStats.sanMax;
            case TIREDNESS:
                return 0;
            case DAMAGELEVEL:
                return ip.roleStats.damageLevel;
            case HEALTHMAX:
                return player.getMaxHealth();
            case LEVEL:
                return ip.roleStats.level;
            case SANMAX:
                return ip.roleStats.sanMax;
        }
        return 0;
    }

    public static double check(Player player, CheckType type) {
        IPlayer ip = players.get(player.getUniqueId());
        if (ip == null || type == null)
            return 0;
        if ((!Config.enableWorlds.contains(player.getWorld()))
                || (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR))
            return getDefault(ip, type, player);
        switch (type) {
            case TEMPERATURE:
                return ip.temperature;
            case HUMIDITY:
                return ip.humidity;
            case SANITY:
                return ip.sanity;
            case TIREDNESS:
                return ip.tiredness;
            case DAMAGELEVEL:
                return ip.roleStats.damageLevel;
            case HEALTHMAX:
                return player.getMaxHealth();
            case LEVEL:
                return ip.roleStats.level;
            case SANMAX:
                return ip.roleStats.sanMax;
        }
        return 1.0;
    }

    public static double check(Player name, String type) {
        return check(name, CheckType.search(type));
    }

    public enum CheckType {
        SANITY("san"), TEMPERATURE("tem"), HUMIDITY("hum"), TIREDNESS("tir"), SANMAX("sanmax"), HEALTHMAX(
                "healthmax"), LEVEL("level"), DAMAGELEVEL("damagelevel");
        private final String sname;

        public String getShortName() {
            return sname;
        }

        CheckType(String shorter) {
            this.sname = shorter;
        }

        public static CheckType search(String name) {
            if (name == null)
                return null;
            CheckType[] val = values();
            for (CheckType c : val) {
                if (c.sname.equalsIgnoreCase(name) || c.name().equalsIgnoreCase(name))
                    return c;
            }
            return null;
        }
    }

    private static BiFunction<String, String, String> buildMarkFunc(String mark) {
        return (k, v) -> {
            if (v.equalsIgnoreCase(mark))
                return " ";
            return v;
        };
    }

    public enum EditAction {
        INCREMENT {
            @Override
            public void update(DoubleSupplier getter, DoubleConsumer setter, double value) {
                setter.accept(getter.getAsDouble() + value);
            }

            @Override
            public void update(IntSupplier getter, IntConsumer setter, int value) {
                setter.accept(getter.getAsInt() + value);
            }
        }, DECREMENT {
            @Override
            public void update(DoubleSupplier getter, DoubleConsumer setter, double value) {
                setter.accept(getter.getAsDouble() - value);
            }

            @Override
            public void update(IntSupplier getter, IntConsumer setter, int value) {
                setter.accept(getter.getAsInt() - value);
            }
        }, SET {
            @Override
            public void update(DoubleSupplier getter, DoubleConsumer setter, double value) {
                setter.accept(value);
            }

            @Override
            public void update(IntSupplier getter, IntConsumer setter, int value) {
                setter.accept(value);
            }
        };

        public void update(DoubleSupplier getter, DoubleConsumer setter, double value) {
        }

        public void update(IntSupplier getter, IntConsumer setter, int value) {
        }
    }

    public static void forgetChange(Player player, CheckType type, double counter) {
        forgetChange(player, type, counter, EditAction.INCREMENT);
    }

    public static void forgetChange(Player player, CheckType type, double counter, EditAction action) {
        if (action == null) action = EditAction.INCREMENT;
        if (player == null)
            return;
        if (type == null)
            return;
        IPlayer ip = players.get(player.getUniqueId());
        String mark;
        if (counter > 0)
            mark = "↑";
        else if (counter < 0)
            mark = "↓";
        else
            mark = " ";
        switch (type) {
            case TEMPERATURE:
                action.update(() -> ip.temperature, v -> ip.temperature = v, counter);
                HudProvider.temperature.put(player.getName(), mark);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        HudProvider.temperature.computeIfPresent(player.getName(), buildMarkFunc(mark));
                    }
                }.runTaskLater(plugin, 40L);
                if (ip.temperature <= 10)
                    player.sendTitle(UTEi18n.cache("mechanism.temperature.to-cool"), "");
                if (ip.temperature >= 60)
                    player.sendTitle(UTEi18n.cache("mechanism.temperature.to-hot"), "");
                if (ip.temperature < -5)
                    ip.temperature = -5;
                if (ip.temperature > 75)
                    ip.temperature = 75;
                break;
            case HUMIDITY:
                action.update(() -> ip.humidity, v -> ip.humidity = v, counter);
                HudProvider.humidity.put(player.getName(), mark);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        HudProvider.humidity.computeIfPresent(player.getName(), buildMarkFunc(mark));
                    }
                }.runTaskLater(plugin, 40L);
                if (ip.humidity < 0)
                    ip.humidity = 0;
                if (ip.humidity > 100)
                    ip.humidity = 100;
                break;
            case SANITY:
                action.update(() -> ip.sanity, v -> ip.sanity = v, counter);
                HudProvider.sanity.put(player.getName(), mark);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        HudProvider.sanity.computeIfPresent(player.getName(), buildMarkFunc(mark));
                    }
                }.runTaskLater(plugin, 40L);
                if (ip.sanity < 0)
                    ip.sanity = 0;
                if (ip.sanity > ip.roleStats.sanMax)
                    ip.sanity = ip.roleStats.sanMax;
                break;
            case TIREDNESS:
                action.update(() -> ip.tiredness, v -> ip.tiredness = v, counter);
                HudProvider.tiredness.put(player.getName(), mark);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        HudProvider.tiredness.computeIfPresent(player.getName(), buildMarkFunc(mark));
                    }
                }.runTaskLater(plugin, 40L);
                if (ip.tiredness < 0)
                    ip.tiredness = 0;
                if (ip.tiredness > 100)
                    ip.tiredness = 100;
                break;
            case DAMAGELEVEL:
                action.update(() -> ip.roleStats.damageLevel, v -> ip.roleStats.damageLevel = v, counter);
                break;
            case HEALTHMAX:
                action.update((IntSupplier) () -> ip.roleStats.healthMax, v -> ip.roleStats.healthMax = v, (int) counter);
                break;
            case LEVEL:
                action.update((IntSupplier) () -> ip.roleStats.level, v -> ip.roleStats.level = v, (int) counter);
                break;
            case SANMAX:
                action.update((IntSupplier) () -> ip.roleStats.sanMax, v -> ip.roleStats.sanMax = v, (int) counter);
                break;
        }
    }

    public static void change(Player player, CheckType type, double changement) {
        change(player, type, changement, EditAction.INCREMENT);
    }

    public static void change(Player player, CheckType type, double changement, EditAction action) {
        if (player == null)
            return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;
        if (player.isDead())
            return;
        forgetChange(player, type, changement, action);
    }

    public static void change(Player player, String type, double changement) {
        change(player, CheckType.search(type), changement);
    }

    private static class SavingTask extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers())
                save(player);
        }

        public SavingTask() {
            runTaskTimer(plugin, 0L, plugin.getConfig().getInt("player.stats.autosave") * 20);
        }
    }

    private static class IPlayer {
        public double temperature;
        public double humidity;
        public double sanity;
        public double tiredness;
        public IRole roleStats;
        public Roles role;

        public IPlayer(double temperature, double humidity, double sanity, double tiredness) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.sanity = sanity;
            this.tiredness = tiredness;
        }
    }
}
