package com.sakurarealm.bukkit.worldborder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderConfig {

    public boolean renderBorderWall;

    public final Map<String, Border> borders;

    private BorderConfig() {
        renderBorderWall = false;
        borders = new HashMap<>();
    }

    public static BorderConfig load(SRWorldBorder plugin) {
        Bukkit.getLogger().info("正在加载边界配置文件...");
        File localConfigYaml = new File(plugin.getDataFolder(), "config.yml");
        if (! localConfigYaml.exists()) {
            Bukkit.getLogger().info("配置文件不存在, 正在创建...");
            plugin.saveDefaultConfig();
        }

        BorderConfig borderConfig = new BorderConfig();
        YamlConfiguration fConfig = YamlConfiguration.loadConfiguration(localConfigYaml);

        borderConfig.renderBorderWall = fConfig.getBoolean("render-border-wall");


        if (!fConfig.contains("borders")) { // Load old version
            return loadOldVersion(plugin, borderConfig, fConfig);
        }

        ConfigurationSection borders = fConfig.getConfigurationSection("borders");
        for (String worldName : borders.getKeys(false)) {
            Bukkit.getLogger().info("正在加载世界 " + worldName + " 的边界信息...");

            ConfigurationSection section = borders.getConfigurationSection(worldName);
            if (section == null) {
                Bukkit.getLogger().warning("世界 " + worldName + " 的边界信息为空, 跳过.");
                continue;
            }
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                Bukkit.getLogger().warning("世界 " + worldName + " 不存在, 跳过.");
                continue;
            }
            Border border = new Border(
                new Location(
                    plugin.getServer().getWorld(section.getString("center.world")),
                    section.getDouble("center.x"),
                    0.f,
                    section.getDouble("center.z")
                ),
                section.getInt("length"),
                section.getInt("width")
            );
            borderConfig.borders.put(worldName, border);

            ConfigurationSection serverSection = section.getConfigurationSection("servers");
            if (serverSection == null)
                continue;
            for (String server : serverSection.getKeys(false)) {
                border.servers.put(server, section.getString(server));
            }
        }

        return borderConfig;
    }

    private static BorderConfig loadOldVersion(SRWorldBorder plugin, BorderConfig borderConfig, YamlConfiguration configurationSection) {
        Bukkit.getLogger().info("正在加载旧版本的配置文件...");

        String worldName = configurationSection.getString("border.world");
        if (worldName == null) {
            Bukkit.getLogger().warning("世界名字空缺, 已重置为world!");
            worldName = "world";
        }

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().warning("世界 " + worldName + " 不存在, 已重置为" + plugin.getServer().getWorlds().get(0).getName());
            world = plugin.getServer().getWorlds().get(0);
        }
        Border border = new Border(
            new Location(
                world,
                configurationSection.getDouble("border.center.x"),
                configurationSection.getDouble("border.center.y"),
                configurationSection.getDouble("border.center.z")
            ),
            configurationSection.getInt("border.length"),
            configurationSection.getInt("border.width")
        );
        borderConfig.borders.put(worldName, border);

        ConfigurationSection section = configurationSection.getConfigurationSection("border.servers");
        if (section == null)
            return borderConfig;
        for (String key : section.getKeys(false)) {
            border.servers.put(key, section.getString(key));
        }

        return borderConfig;
    }

    public String getBorderInfo() {
        StringBuilder res = new StringBuilder(ChatColor.GOLD + "边界信息:" + "\n");
        for (BorderConfig.Border border : getBorders()) {
            res.append(ChatColor.GREEN).append("世界名").append(ChatColor.WHITE)
                    .append(border.getCenter().getWorld().getName()).append("\n")

                    .append(ChatColor.GREEN).append("中心: ").append(ChatColor.WHITE).append("X: ")
                    .append(border.getCenter().getX()).append(", Z: ").append(border.getCenter().getZ()).append("\n")

                    .append(ChatColor.GREEN).append("长度: ").append(ChatColor.WHITE).append(border.getLength()).append("\n")
                    .append(ChatColor.GREEN).append("宽度: ").append(ChatColor.WHITE).append(border.getWidth()).append("\n")

                    .append(ChatColor.GREEN).append("北边服务器: ").append(ChatColor.WHITE)
                    .append(border.getServers().get("north")).append("\n")
                    .append(ChatColor.GREEN).append("南边服务器: ").append(ChatColor.WHITE)
                    .append(border.getServers().get("south")).append("\n")
                    .append(ChatColor.GREEN).append("东边服务器: ").append(ChatColor.WHITE)
                    .append(border.getServers().get("east")).append("\n")
                    .append(ChatColor.GREEN).append("西边服务器: ").append(ChatColor.WHITE)
                    .append(border.getServers().get("west")).append("\n");
        }
        return res.toString();
    }

    public int getBorderLength(String worldName) {
        Border border = borders.get(worldName);
        if (border == null)
            throw new RuntimeException(worldName + "没有边界.");
        return border.getLength();
    }

    public int getBorderWidth(String worldName) {
        Border border = borders.get(worldName);
        if (border == null)
            throw new RuntimeException(worldName + "没有边界.");
        return border.getWidth();
    }

    public Location getBorderCenter(String worldName) {
        Border border = borders.get(worldName);
        if (border == null)
            throw new RuntimeException(worldName + "没有边界.");
        return border.getCenter();
    }

    public boolean hasBorder(String worldName) {
        return borders.containsKey(worldName);
    }

    public Border getBorder(String worldName) {
        return borders.get(worldName);
    }

    public List<Border> getBorders() {
        return (List<Border>) borders.values();
    }

    public static class Border {
        public Location center;
        public int length;
        public int width;
        public final Map<String, String> servers;

        private Border(Location center, int length, int width) {
            this.center = center.clone();
            this.length = length;
            this.width = width;
            this.servers = new HashMap<>();
        }

        public Location getCenter() {
            return center;
        }

        public int getLength() {
            return length;
        }

        public int getWidth() {
            return width;
        }

        public Map<String, String> getServers() {
            return servers;
        }
    }

}
