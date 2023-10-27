package com.sakurarealm.bukkit.worldborder.utils;

import com.sakurarealm.bukkit.worldborder.BorderConfig;
import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class WallRenderer {

    private static final int DISTANCE_TO_BORDER = 10;
    private static final int RENDER_WALL_LENGTH = 10;
    private static final int RENDER_WALL_HEIGHT = 10;

    private final int task;

    BorderConfig borderConfig;

    public WallRenderer(BorderConfig borderConfig) {
        this.borderConfig = borderConfig;
        task = SRWorldBorder.getInstance().getServer().getScheduler()
                .scheduleSyncRepeatingTask(
                        SRWorldBorder.getInstance(),
                        this::checkPlayerDistanceToBorder,
                        0,
                        20
                );
    }

    public void stop () {
        SRWorldBorder.getInstance().getServer().getScheduler().cancelTask(task);
    }

    private void checkPlayerDistanceToBorder() {
        if (!borderConfig.renderBorderWall)
            return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            Location borderCenter = borderConfig.getBorderCenter(playerLocation.getWorld().getName());
            int borderLength = borderConfig.getBorderLength(playerLocation.getWorld().getName());
            int borderWidth = borderConfig.getBorderWidth(playerLocation.getWorld().getName());
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
        Location borderCenter = borderConfig.getBorderCenter(player.getLocation().getWorld().getName());
        int borderLength = borderConfig.getBorderLength(player.getLocation().getWorld().getName());
        int borderWidth = borderConfig.getBorderWidth(player.getLocation().getWorld().getName());
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
        Location borderCenter = borderConfig.getBorderCenter(player.getLocation().getWorld().getName());
        int borderLength = borderConfig.getBorderLength(player.getLocation().getWorld().getName());
        int borderWidth = borderConfig.getBorderWidth(player.getLocation().getWorld().getName());
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
        Location borderCenter = borderConfig.getBorderCenter(player.getLocation().getWorld().getName());
        int borderLength = borderConfig.getBorderLength(player.getLocation().getWorld().getName());
        int borderWidth = borderConfig.getBorderWidth(player.getLocation().getWorld().getName());
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
        Location borderCenter = borderConfig.getBorderCenter(player.getLocation().getWorld().getName());
        int borderLength = borderConfig.getBorderLength(player.getLocation().getWorld().getName());
        int borderWidth = borderConfig.getBorderWidth(player.getLocation().getWorld().getName());
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

}
