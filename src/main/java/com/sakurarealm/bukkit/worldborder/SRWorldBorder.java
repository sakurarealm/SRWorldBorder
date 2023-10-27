package com.sakurarealm.bukkit.worldborder;

import com.sakurarealm.bukkit.worldborder.commands.BorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.commands.BorderControlCommand;
import com.sakurarealm.bukkit.worldborder.commands.TempBorderBypassCommand;
import com.sakurarealm.bukkit.worldborder.listener.BorderListener;
import com.sakurarealm.bukkit.worldborder.utils.BossBarManager;
import com.sakurarealm.bukkit.worldborder.utils.BypassPermissionManager;
import com.sakurarealm.bukkit.worldborder.utils.FindNearestAir;
import com.sakurarealm.bukkit.worldborder.utils.WallRenderer;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

    private Map<UUID, Location> lastValidLocations;
    private Map<UUID, Integer> countdowns;

    private BorderConfig borderConfig;

    private WallRenderer wallRenderer;

    private BossBarManager bossBarManager;

    private BypassPermissionManager bypassPermissionManager;



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
        Bukkit.getLogger().info("SRWorldBorder is loading...");
        if (!checkLuckPerms()) {
            getLogger().severe("LuckPerms not found! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        INSTANCE = this;

        bossBarManager = new BossBarManager();

        saveDefaultConfig();
        loadBorderConfig();

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

    public void loadBorderConfig() {
        borderConfig = BorderConfig.load(this);
        if (wallRenderer != null)
            wallRenderer.stop();
        wallRenderer = null;
        if (borderConfig.renderBorderWall)
            wallRenderer = new WallRenderer(borderConfig);
    }

    public BorderConfig getBorderConfig() {
        return borderConfig;
    }

    public BypassPermissionManager getBypassPermissionManager() {
        return bypassPermissionManager;
    }

    private void redirectPlayer(Player player, String server) {
        Bukkit.getLogger().info("[Border] Redirect player to " + server);
        // player.sendMessage(server);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("redirect %s %s", player.getName(), server));
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
            String worldName = player.getLocation().getWorld().getName();
            if (!borderConfig.hasBorder(worldName)) {
                return;
            }
            Location borderCenter = borderConfig.getBorderCenter(worldName);
            if (bypassPermissionManager.hasPermission(player, "srwb.bypass"))
                return;

            int borderLength = borderConfig.getBorderLength(worldName);
            int borderWidth = borderConfig.getBorderWidth(worldName);

            Location loc = player.getLocation();
            double deltaX = loc.getX() - borderCenter.getX();
            double deltaZ = loc.getZ() - borderCenter.getZ();


            // 检查玩家是否在边境中
            if (Math.abs(deltaX) > borderLength / 2. || Math.abs(deltaZ) > borderWidth / 2.) { // 玩家在边界外
                // 获取玩家位于的边界的方向
                String direction = null;
                if (deltaX > (double) borderLength / 2) direction = "east";
                else if (deltaX < (double) -borderLength / 2) direction = "west";
                else if (deltaZ > (double) borderWidth / 2) direction = "south";
                else if (deltaZ < (double) -borderWidth / 2) direction = "north";

                // 获取玩家应该传送到的服务器
                String server;
                if (direction != null)
                    server = borderConfig.getBorder(worldName).getServers().get(direction);
                else {
                    server = null;
                }

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
                UUID playerId = player.getUniqueId();
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

        String worldName = currentLoc.getWorld().getName();
        Location borderCenter = borderConfig.getBorderCenter(worldName);
        int borderLength = borderConfig.getBorderLength(worldName);
        int borderWidth = borderConfig.getBorderWidth(worldName);

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
