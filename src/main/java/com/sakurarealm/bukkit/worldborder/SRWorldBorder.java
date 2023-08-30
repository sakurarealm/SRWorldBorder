package com.sakurarealm.bukkit.worldborder;

import com.sakurarealm.bukkit.worldborder.commands.BorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.commands.BorderControlCommand;
import com.sakurarealm.bukkit.worldborder.commands.TempBorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.listener.BorderListener;
import com.sakurarealm.bukkit.worldborder.utils.BossBarManager;
import com.sakurarealm.bukkit.worldborder.utils.FindNearestAir;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class SRWorldBorder extends JavaPlugin implements Listener {

    private static SRWorldBorder INSTANCE;

    private boolean renderBorderWall;
    private Location borderCenter;
    private int borderLength;
    private int borderWidth;
    private Map<String, String> servers;

    private Map<UUID, Location> lastValidLocations;
    private Map<UUID, Integer> countdowns;

    private BossBarManager bossBarManager;

    private static final int DISTANCE_TO_BORDER = 10;
    private static final int RENDER_WALL_LENGTH = 10;
    private static final int RENDER_WALL_HEIGHT = 10;

    public static SRWorldBorder getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        bossBarManager = new BossBarManager();

        saveDefaultConfig();
        loadConfig();

        // 初始化所有命令
        getCommand("bordercontrol").setExecutor(new BorderControlCommand());
        getCommand("borderbypass").setExecutor(new BorderBypassCommand());
        getCommand("tempborderbypass").setExecutor(new TempBorderBypassCommand());

        // 初始化所有监听器
        Bukkit.getPluginManager().registerEvents(new BorderListener(), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        countdowns = new HashMap<>();
        lastValidLocations = new HashMap<>();

        // 注册主任务
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::checkPlayerDistanceToBorder, 0,20);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::checkPlayerBorder, 0, 20);
    }

    @Override
    public void onDisable() {
        bossBarManager.removeAllBossBars();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teleportPlayerToValidLocation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastValidLocations.remove(event.getPlayer().getUniqueId());
    }

    public void loadConfig() {
        reloadConfig();
        renderBorderWall = getConfig().getBoolean("render-border-wall", true);
        borderCenter = new Location(
                getServer().getWorld(getConfig().getString("border.world")),
                getConfig().getDouble("border.center.x"),
                getConfig().getDouble("border.center.y"),
                getConfig().getDouble("border.center.z")
        );
        borderLength = getConfig().getInt("border.length");
        borderWidth = getConfig().getInt("border.width");
        servers = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("border.servers");
        for (String key : section.getKeys(false)) {
            servers.put(key, section.getString(key));
        }
    }

    public int getBorderLength() {
        return borderLength;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public Location getBorderCenter() {
        return borderCenter;
    }

    public Map<String, String> getServers() {
        return servers;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public String getBorderInfo() {
        StringBuilder info = new StringBuilder();
        info.append(ChatColor.GOLD).append("边界信息:").append("\n");
        info.append(ChatColor.GREEN).append("中心: ").append(ChatColor.WHITE).append("X: ").append(borderCenter.getX()).append(", Z: ").append(borderCenter.getZ()).append("\n");
        info.append(ChatColor.GREEN).append("长度: ").append(ChatColor.WHITE).append(borderLength).append("\n");
        info.append(ChatColor.GREEN).append("宽度: ").append(ChatColor.WHITE).append(borderWidth).append("\n");

        info.append(ChatColor.GREEN).append("北边服务器: ").append(ChatColor.WHITE).append(servers.get("north")).append("\n");
        info.append(ChatColor.GREEN).append("南边服务器: ").append(ChatColor.WHITE).append(servers.get("south")).append("\n");
        info.append(ChatColor.GREEN).append("东边服务器: ").append(ChatColor.WHITE).append(servers.get("east")).append("\n");
        info.append(ChatColor.GREEN).append("西边服务器: ").append(ChatColor.WHITE).append(servers.get("west")).append("\n");

        return info.toString();
    }

    private void redirectPlayer(Player player, String server) {
        Bukkit.getLogger().info("[Border] Redirect player to " + server);
        // player.sendMessage(server);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("redirect %s %s", player.getName(), server));
    }

    private void checkPlayerDistanceToBorder() {
        if (!renderBorderWall)
            return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            double distanceToNorth = Math.abs(playerLocation.getZ() - (borderCenter.getZ() - borderWidth / 2));
            double distanceToSouth = Math.abs(playerLocation.getZ() - (borderCenter.getZ() + borderWidth / 2));
            double distanceToEast = Math.abs(playerLocation.getX() - (borderCenter.getX() + borderLength / 2));
            double distanceToWest = Math.abs(playerLocation.getX() - (borderCenter.getX() - borderLength / 2));

            if (distanceToNorth <= DISTANCE_TO_BORDER) showNorthWall(player);
            if (distanceToSouth <= DISTANCE_TO_BORDER) showSouthWall(player);
            if (distanceToEast <= DISTANCE_TO_BORDER) showEastWall(player);
            if (distanceToWest <= DISTANCE_TO_BORDER) showWestWall(player);
        }
    }

    private void showNorthWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT; // 墙的高度
        double z = borderCenter.getZ() - borderWidth / 2;

        double xBorderMax = Math.min(borderCenter.getX() + borderLength / 2, borderCenter.getBlockX() + RENDER_WALL_LENGTH / 2);
        double xBorderMin = Math.max(borderCenter.getX() - borderLength / 2, borderCenter.getBlockX() - RENDER_WALL_LENGTH);

        for (int y = yMin; y < yMax; y++) {
            for (int x = (int) xBorderMin; x <= xBorderMax; x++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showSouthWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double z = borderCenter.getZ() + borderWidth / 2;

        double xBorderMax = Math.min(borderCenter.getX() + borderLength / 2, player.getLocation().getBlockX() + RENDER_WALL_LENGTH / 2);
        double xBorderMin = Math.max(borderCenter.getX() - borderLength / 2, player.getLocation().getBlockX() - RENDER_WALL_LENGTH / 2);

        for (int y = yMin; y < yMax; y++) {
            for (int x = (int) xBorderMin; x <= xBorderMax; x++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showEastWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double x = borderCenter.getX() + borderLength / 2;

        double zBorderMax = Math.min(borderCenter.getZ() + borderWidth / 2, player.getLocation().getBlockZ() + RENDER_WALL_LENGTH / 2);
        double zBorderMin = Math.max(borderCenter.getZ() - borderWidth / 2, player.getLocation().getBlockZ() - RENDER_WALL_LENGTH / 2);

        for (int y = yMin; y < yMax; y++) {
            for (int z = (int) zBorderMin; z <= zBorderMax; z++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showWestWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double x = borderCenter.getX() - borderLength / 2;

        double zBorderMax = Math.min(borderCenter.getZ() + borderWidth / 2, player.getLocation().getBlockZ() + RENDER_WALL_LENGTH / 2);
        double zBorderMin = Math.max(borderCenter.getZ() - borderWidth / 2, player.getLocation().getBlockZ() - RENDER_WALL_LENGTH / 2);

        for (int y = yMin; y < yMax; y++) {
            for (int z = (int) zBorderMin; z <= zBorderMax; z++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void checkPlayerBorder() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("srwb.bypass") ||
                !player.getLocation().getWorld().getName().equalsIgnoreCase(borderCenter.getWorld().getName()))
                return;

            Location loc = player.getLocation();
            double deltaX = loc.getX() - borderCenter.getX();
            double deltaZ = loc.getZ() - borderCenter.getZ();

            UUID playerId = player.getUniqueId();

            String direction = null;
            if (deltaX > borderLength / 2) direction = "east";
            else if (deltaX < -borderLength / 2) direction = "west";
            else if (deltaZ > borderWidth / 2) direction = "south";
            else if (deltaZ < -borderWidth / 2) direction = "north";
            String server = null;
            if (direction != null)
                server = servers.get(direction);

            // 检查玩家是否在边境中
            if (Math.abs(deltaX) > borderLength / 2 || Math.abs(deltaZ) > borderWidth / 2) {
                // 判断这边是否有server
                if (server != null) {
                    // 将走出边界的玩家设置成冒险者模式
//                    if (player.getGameMode() != GameMode.ADVENTURE) {
//                        player.setGameMode(GameMode.ADVENTURE);
//                    }
                    // 开始倒计时
                    if (!countdowns.containsKey(playerId)) {
                        countdowns.put(playerId, 10);
                        bossBarManager.sendBossBar(player, "您已跨越边界，将于 %ds 后前往对应大陆", BarColor.RED, BarStyle.SOLID, 1.0);
                    }
                } else {
                    Location lastValidLocation = lastValidLocations.get(player.getUniqueId());
                    // 将玩家传送回上一次记录的有效位置
                    if (lastValidLocation != null) {
                        player.sendMessage(ChatColor.DARK_RED + "您已抵达这个大陆的边界!");
                        teleportPlayerToValidLocation(player);
                        // player.teleport(lastValidLocation);
                    } else {
                        // 如果玩家出生在了边境外, 将玩家传送到世界中心
                        teleportPlayerToValidLocation(player);
                        // player.teleport(getBorderCenter());
                    }
                }

            } else {
                // 取消玩家正在进行的倒计时
                if (countdowns.containsKey(playerId)) {
                    // 取消倒计时
                    countdowns.remove(playerId);
                    bossBarManager.removeBossBar(player);
                }
                // 更新玩家最后的有效位置
                lastValidLocations.put(playerId, loc);
                // 更新玩家回到生存模式
//                if (player.getGameMode() == GameMode.ADVENTURE) {
//                    player.setGameMode(GameMode.SURVIVAL);
//                }
            }
            // 继续倒数，如果玩家没有回到边界内
            if (countdowns.containsKey(playerId)) {
                int timeLeft = countdowns.get(playerId) - 1;
                countdowns.put(playerId, timeLeft);

                // 更新Boss血条
                bossBarManager.updateBossBarProgress(player, String.format("您已跨越边界，将于 %ds 后前往对应大陆", timeLeft), timeLeft / 10f);

                if (timeLeft <= 0) {
                    countdowns.remove(playerId);
                    bossBarManager.removeBossBar(player);

                    // 进行传送
                    if (server != null) {
                        teleportPlayerToValidLocation(player);
                        redirectPlayer(player, server);
                    }


                    Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
                        @Override
                        public void run() {
                            // 如果玩家没有跨服成功
                            if (player.isOnline()) {
                                Location lastValidLocation = lastValidLocations.get(player.getUniqueId());
                                player.teleport(lastValidLocation);
                                countdowns.remove(playerId);
                                bossBarManager.removeBossBar(player);
                            }
                        }
                    }, 20L);
                }
            }
        }
    }

    private void teleportPlayerToValidLocation(Player player) {

        Location loc = lastValidLocations.get(player.getUniqueId());

        if (loc != null) {
            player.teleport(loc);
            return;
        }

        Location newLoc = newLocation(player.getLocation(), 5);

        if (newLoc == null) {
            Bukkit.getLogger().warning("Could not find any valid location for player teleportation!");
            return;
        }

        player.teleport(newLoc);
    }

    private Location newLocation(Location currentLoc, int radius) {
        Location inBorderLoc = currentLoc.clone();

        double deltaX = currentLoc.getX() - borderCenter.getX();
        double deltaZ = currentLoc.getZ() - borderCenter.getZ();

        // 将玩家放置回边境内
        if (deltaX > borderLength / 2) inBorderLoc.setX(borderLength / 2 + borderCenter.getX() - radius);
        else if (deltaX < -borderLength / 2) inBorderLoc.setX(-borderLength / 2 + borderCenter.getX() + radius);
        if (deltaZ > borderWidth / 2) inBorderLoc.setZ(borderWidth / 2 + borderCenter.getZ() - radius);
        else if (deltaZ < -borderWidth / 2) inBorderLoc.setZ(-borderWidth / 2 + borderCenter.getZ() + radius);


        FindNearestAir bfs = new FindNearestAir(inBorderLoc, radius);
        return bfs.find();
    }

}
