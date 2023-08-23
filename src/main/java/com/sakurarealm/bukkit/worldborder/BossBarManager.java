package com.sakurarealm.bukkit.worldborder;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class BossBarManager {
    private final HashMap<UUID, BossBar> playerBossBars = new HashMap<>();

    public void sendBossBar(Player player, String title, BarColor color, BarStyle style, double progress) {
        removeBossBar(player); // 移除现有的boss血条（如果存在）

        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setProgress(progress);
        bossBar.addPlayer(player);
        playerBossBars.put(player.getUniqueId(), bossBar);
    }

    public void updateBossBarProgress(Player player,  String title, double progress) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setTitle(title);
            bossBar.setProgress(progress);
        }
    }

    public void removeBossBar(Player player) {
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    public void removeAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
    }
}
