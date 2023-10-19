package com.sakurarealm.bukkit.worldborder.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.time.Duration;

public class BypassPermissionManager {

    LuckPerms luckPerms;

    public BypassPermissionManager(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public boolean hasPermission(Player player, String permission) {
        User user = luckPerms.getUserManager().getUser(player.getName());
        if (user != null) {
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        }
        return false;
    }

    public PermissionType getPermissionType(Player player, String permission) {
        User user = luckPerms.getUserManager().getUser(player.getName());
        if (user != null) {
            for (Node node : user.getNodes()) {
                if (node.getKey().equalsIgnoreCase(permission)) {
                    if (node.hasExpiry()) { // 有过期时间的权限
                        return PermissionType.TEMPORARY;
                    } else {
                        return PermissionType.PERMANENT;
                    }
                }
            }
        }
        return PermissionType.NONE; // 没有权限
    }

    /**
     * 为玩家添加限时bypass权限
     *
     * @param player        玩家
     * @param timeInSeconds 限时权限持续时间（秒), 0为永久
     * @return 是否成功添加权限
     */
    public boolean givePlayerBypassPermission(Player player, int timeInSeconds) {
        User user = luckPerms.getUserManager().getUser(player.getName());
        if (user == null) {
            throw new RuntimeException("玩家 " + player.getName() + " 不存在。");
        } else if (timeInSeconds < 0) {
            throw new IllegalArgumentException("timeInSeconds 必须大于等于0。");
        }

        // 为玩家添加永久bypass权限
        if (timeInSeconds == 0) {
            Node permNode = Node.builder("srwb.bypass").build();
            user.data().add(permNode);
            luckPerms.getUserManager().saveUser(user);
            return true;
        }

        // 检查玩家是否有永久权限, 有则不添加限时权限
        if (getPermissionType(player, "srwb.bypass") == PermissionType.PERMANENT) {
            return false;
        }

        // 为玩家添加限时bypass权限
        Duration duration = Duration.ofSeconds(timeInSeconds);
        Node tempNode = Node.builder("srwb.bypass")
                .expiry(duration)
                .build();

        user.data().add(tempNode);
        luckPerms.getUserManager().saveUser(user);

        return true;
    }

    /**
     * 为玩家移除永久或限时bypass权限
     *
     * @param player 玩家
     * @return 是否成功移除权限
     */
    public boolean removePlayerBypassPermission(Player player, boolean onlyRemoveTemporary) {
        User user = luckPerms.getUserManager().getUser(player.getName());
        if (user == null) {
            throw new RuntimeException("玩家 " + player.getName() + " 不存在。");
        }

        if (!hasPermission(player, "srwb.bypass")) {
            return true;
        }
        if (onlyRemoveTemporary) {
            // 移除限时bypass权限
            if (getPermissionType(player, "srwb.bypass") == PermissionType.TEMPORARY) {
                Node permNode = Node.builder("srwb.bypass").build();
                user.data().remove(permNode);
                luckPerms.getUserManager().saveUser(user);
                return true;
            } else {
                return false;
            }
        } else {
            // 移除永久bypass权限
            Node permNode = Node.builder("srwb.bypass").build();
            user.data().remove(permNode);
            luckPerms.getUserManager().saveUser(user);
            return true;
        }
    }

    public enum PermissionType {
        TEMPORARY,
        PERMANENT,
        NONE
    }
}
