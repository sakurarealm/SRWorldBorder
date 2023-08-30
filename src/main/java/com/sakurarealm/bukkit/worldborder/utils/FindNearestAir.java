package com.sakurarealm.bukkit.worldborder.utils;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.LinkedList;
import java.util.Queue;

public class FindNearestAir {

    private static final int[] dx = {-1, 1, 0, 0, 0, 0};
    private static final int[] dy = {0, 0, -1, 1, 0, 0};
    private static final int[] dz = {0, 0, 0, 0, -1, 1};

    private final Location start;
    private final int radius;
    private final int range;

    private boolean[][][] gray;

    public FindNearestAir(Location start, int radius) {
        this.radius = radius;
        this.start = start.clone();
        this.range = radius * 2 + 1;
    }

    public Location find() {
        this.gray = new boolean[range][range][range];
        this.gray[radius][radius][radius] = true;

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(0, 0, 0));

        while (!queue.isEmpty()) {
            Point loc = queue.poll();

            Location check = getOffsetLocation(loc);
            if (check.getBlock().getType() == Material.AIR)
                return check;

            for (int i=0; i<6; i++) {
                int x = loc.x + dx[i];
                int y = loc.y + dy[i];
                int z = loc.z + dz[i];
                Point neighbor = new Point(x, y, z);
                if (inRange(neighbor) && !isVisited(neighbor)) {
                    gray[x][y][z] = true;
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private Location getOffsetLocation(Point offset) {
        return start.clone().add(offset.x, offset.y, offset.z);
    }

    private boolean inRange(Point offset) {
        return Math.abs(offset.x) <= radius && Math.abs(offset.y) <= radius && Math.abs(offset.z) <= radius;
    }

    private boolean isVisited(Point point) {
        return gray[point.x + radius][point.y + radius][point.z + radius];
    }

    private static class Point {
        int x, y, z;
        Point(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
