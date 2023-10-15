package com.sakurarealm.bukkit.worldborder;

import com.sakurarealm.bukkit.worldborder.commands.BorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.commands.BorderControlCommand;
import com.sakurarealm.bukkit.worldborder.commands.TempBorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.listener.BorderListener;
import com.sakurarealm.bukkit.worldborder.utils.BossBarManager;
import com.sakurarealm.bukkit.worldborder.utils.BypassPermissionManager;
import com.sakurarealm.bukkit.worldborder.utils.FindNearestAir;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private BypassPermissionManager bypassPermissionManager;

    private static final int DISTANCE_TO_BORDER = 10;
    private static final int RENDER_WALL_LENGTH = 10;
    private static final int RENDER_WALL_HEIGHT = 10;

    public static SRWorldBorder getInstance() {
        return INSTANCE;
    }

    private boolean checkLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            bypassPermissionManager = new BypassPermissionManager(provider.getProvider());
            return true;
        }
        return false;
    }

    @Override
    public void onEnable() {
        if (!checkLuckPerms()) {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

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

    public BypassPermissionManager getBypassPermissionManager() {
        return bypassPermissionManager;
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

    public String getBorderInfo() {
        return ChatColor.GOLD + "边界信息:" + "\n" +
                ChatColor.GREEN + "中心: " + ChatColor.WHITE + "X: " + borderCenter.getX() + ", Z: " + borderCenter.getZ() + "\n" +
                ChatColor.GREEN + "长度: " + ChatColor.WHITE + borderLength + "\n" +
                ChatColor.GREEN + "宽度: " + ChatColor.WHITE + borderWidth + "\n" +
                ChatColor.GREEN + "北边服务器: " + ChatColor.WHITE + servers.get("north") + "\n" +
                ChatColor.GREEN + "南边服务器: " + ChatColor.WHITE + servers.get("south") + "\n" +
                ChatColor.GREEN + "东边服务器: " + ChatColor.WHITE + servers.get("east") + "\n" +
                ChatColor.GREEN + "西边服务器: " + ChatColor.WHITE + servers.get("west") + "\n";
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
            double distanceToNorth = Math.abs(playerLocation.getZ() - (borderCenter.getZ() - borderWidth / 2.));
            double distanceToSouth = Math.abs(playerLocation.getZ() - (borderCenter.getZ() + borderWidth / 2.));
            double distanceToEast = Math.abs(playerLocation.getX() - (borderCenter.getX() + borderLength / 2.));
            double distanceToWest = Math.abs(playerLocation.getX() - (borderCenter.getX() - borderLength / 2.));

            if (distanceToNorth <= DISTANCE_TO_BORDER) showNorthWall(player);
            if (distanceToSouth <= DISTANCE_TO_BORDER) showSouthWall(player);
            if (distanceToEast <= DISTANCE_TO_BORDER) showEastWall(player);
            if (distanceToWest <= DISTANCE_TO_BORDER) showWestWall(player);
        }
    }

    private void showNorthWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT; // 墙的高度
        double z = borderCenter.getZ() - (double) borderWidth / 2;

        double xBorderMax = Math.min(borderCenter.getX() + borderLength / 2.,
                borderCenter.getBlockX() + RENDER_WALL_LENGTH / 2.);
        double xBorderMin = Math.max(borderCenter.getX() - borderLength / 2.,
                borderCenter.getBlockX() - RENDER_WALL_LENGTH);

        for (int y = yMin; y < yMax; y++) {
            for (int x = (int) xBorderMin; x <= xBorderMax; x++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showSouthWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double z = borderCenter.getZ() + (double) borderWidth / 2;

        double xBorderMax = Math.min(borderCenter.getX() + borderLength / 2.,
                player.getLocation().getBlockX() + RENDER_WALL_LENGTH / 2.);
        double xBorderMin = Math.max(borderCenter.getX() - borderLength / 2.,
                player.getLocation().getBlockX() - RENDER_WALL_LENGTH / 2.);

        for (int y = yMin; y < yMax; y++) {
            for (int x = (int) xBorderMin; x <= xBorderMax; x++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showEastWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double x = borderCenter.getX() + borderLength / 2.;

        double zBorderMax = Math.min(borderCenter.getZ() + borderWidth / 2.,
                player.getLocation().getBlockZ() + RENDER_WALL_LENGTH / 2.);
        double zBorderMin = Math.max(borderCenter.getZ() - borderWidth / 2.,
                player.getLocation().getBlockZ() - RENDER_WALL_LENGTH / 2.);

        for (int y = yMin; y < yMax; y++) {
            for (int z = (int) zBorderMin; z <= zBorderMax; z++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void showWestWall(Player player) {
        int yMin = player.getLocation().getBlockY() - RENDER_WALL_HEIGHT / 2;
        int yMax = yMin + RENDER_WALL_HEIGHT;
        double x = borderCenter.getX() - borderLength / 2.;

        double zBorderMax = Math.min(borderCenter.getZ() + borderWidth / 2.,
                player.getLocation().getBlockZ() + RENDER_WALL_LENGTH / 2.);
        double zBorderMin = Math.max(borderCenter.getZ() - borderWidth / 2.,
                player.getLocation().getBlockZ() - RENDER_WALL_LENGTH / 2.);

        for (int y = yMin; y < yMax; y++) {
            for (int z = (int) zBorderMin; z <= zBorderMax; z++) {
                player.spawnParticle(Particle.BARRIER, x, y, z, 0);
            }
        }
    }

    private void updateCountDown(Player player, String server) {
        // 开始倒计时
        if (!countdowns.containsKey(player.getUniqueId())) {
            countdowns.put(player.getUniqueId(), 10);
            bossBarManager.sendBossBar(player,
                    "您已跨越边界，将于 %ds 后前往对应大陆", BarColor.RED, BarStyle.SOLID, 1.0);
        } else {
            // 继续倒数，如果玩家没有回到边界内，那么就传送玩家
            int timeLeft = countdowns.get(player.getUniqueId()) - 1;
            countdowns.put(player.getUniqueId(), timeLeft);

            // 更新Boss血条
            bossBarManager.updateBossBarProgress(player,
                    String.format("您已跨越边界，将于 %ds 后前往对应大陆", timeLeft),
                    timeLeft / 10f);

            if (timeLeft <= 0) {
                countdowns.remove(player.getUniqueId());
                bossBarManager.removeBossBar(player);

                // 进行传送, 2 tick后传送
                if (server != null) {
                    teleportPlayerToValidLocation(player);

                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline())
                            redirectPlayer(player, server);
                    }, 2L);
                }

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    // 如果玩家没有跨服成功
                    if (player.isOnline()) {
                        Location lastValidLocation = lastValidLocations.get(player.getUniqueId());
                        player.teleport(lastValidLocation);
                        countdowns.remove(player.getUniqueId());
                        bossBarManager.removeBossBar(player);
                    }
                }, 5L);
            }
        }
    }

    private void checkPlayerBorder() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bypassPermissionManager.hasPermission(player, "srwb.bypass") ||
                !player.getLocation().getWorld().getName().equalsIgnoreCase(borderCenter.getWorld().getName()))
                return;

            Location loc = player.getLocation();
            double deltaX = loc.getX() - borderCenter.getX();
            double deltaZ = loc.getZ() - borderCenter.getZ();

            UUID playerId = player.getUniqueId();

            String direction = null;
            if (deltaX > (double) borderLength / 2) direction = "east";
            else if (deltaX < (double) -borderLength / 2) direction = "west";
            else if (deltaZ > (double) borderWidth / 2) direction = "south";
            else if (deltaZ < (double) -borderWidth / 2) direction = "north";

            // 获取玩家应该传送到的服务器
            String server;
            if (direction != null)
                server = servers.get(direction);
            else {
                server = null;
            }

            // 检查玩家是否在边境中
            if (Math.abs(deltaX) > borderLength / 2. || Math.abs(deltaZ) > borderWidth / 2.) { // 玩家在边界外
                // 判断这边是否有server
                if (server != null) {
                    // 有server，开始倒计时
                    updateCountDown(player, server);
                } else {
                    // 没有server，玩家已经到达边界
                    player.sendMessage(ChatColor.DARK_RED + "您已抵达这个大陆的边界!");
                    teleportPlayerToValidLocation(player);
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
            }
        }
    }

    private void teleportPlayerToValidLocation(Player player) {

        Location loc = lastValidLocations.get(player.getUniqueId());

        if (loc != null) {
            player.teleport(loc);
            return;
        }

        Location newLoc = newLocation(player.getLocation(), 10);

        if (newLoc == null) {
            Bukkit.getLogger().warning("Could not find any valid location for player teleportation!");
            player.sendMessage(ChatColor.DARK_RED + "无法找到有效tp未知, 请联系管理!");
            return;
        }

        lastValidLocations.put(player.getUniqueId(), newLoc);
        player.teleport(newLoc);
    }

    private Location newLocation(Location currentLoc, int radius) {
        Location inBorderLoc = currentLoc.clone();

        double deltaX = currentLoc.getX() - borderCenter.getX();
        double deltaZ = currentLoc.getZ() - borderCenter.getZ();

        // 将玩家放置回边境内
        if (deltaX > (double) borderLength / 2) inBorderLoc.setX((double) borderLength / 2 + borderCenter.getX() - radius);
        else if (deltaX < (double) -borderLength / 2) inBorderLoc.setX((double) -borderLength / 2 + borderCenter.getX() + radius);
        if (deltaZ > (double) borderWidth / 2) inBorderLoc.setZ((double) borderWidth / 2 + borderCenter.getZ() - radius);
        else if (deltaZ < (double) -borderWidth / 2) inBorderLoc.setZ((double) -borderWidth / 2 + borderCenter.getZ() + radius);


        FindNearestAir bfs = new FindNearestAir(inBorderLoc, radius);
        return bfs.find();
    }

}
