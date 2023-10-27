package com.sakurarealm.bukkit.worldborder.listener;

import com.sakurarealm.bukkit.worldborder.BorderConfig;
import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BorderListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isLocationOutsideBorder(event.getBlock().getLocation())) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.DARK_RED + "你不能在边界外破坏方块！");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isLocationOutsideBorder(event.getBlock().getLocation())) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.DARK_RED +"你不能在边界外放置方块！");
            event.setCancelled(true);
        }
    }

    private boolean isLocationOutsideBorder(Location location) {
        String worldName = location.getWorld().getName();
        BorderConfig borderConfig = SRWorldBorder.getInstance().getBorderConfig();
        double borderLength = borderConfig.getBorderLength(worldName);
        double borderWidth = borderConfig.getBorderWidth(worldName);
        Location borderCenter = borderConfig.getBorderCenter(worldName);
        return Math.abs(location.getX() - borderCenter.getX()) > borderLength / 2 ||
                Math.abs(location.getZ() - borderCenter.getZ()) > borderWidth / 2;
    }
}
