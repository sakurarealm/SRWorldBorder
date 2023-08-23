package com.sakurarealm.bukkit.worldborder.commands;

import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TempBorderBypassCommand implements CommandExecutor {
    private final Map<UUID, BukkitTask> tempBypassTasks = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("srwb.admin")) {
                sender.sendMessage(ChatColor.DARK_RED + "你没有权限执行此命令。");
                return true;
            }
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.DARK_RED + "使用方法：/tempborderbypass <玩家名> <true|false> <时间(秒)>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.DARK_RED + "指定的玩家不在线。");
            return true;
        }

        boolean enableBypass = args[1].equalsIgnoreCase("true");
        int timeInSeconds;
        try {
            timeInSeconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.DARK_RED + "无效的时间。");
            return true;
        }

        if (enableBypass) {
            PermissionAttachment attachment = target.addAttachment(SRWorldBorder.getInstance(), "srwb.bypass", true);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " 现在拥有限时bypass权限，持续 " + timeInSeconds + " 秒。");

            // 定时任务来移除权限
            BukkitTask task = Bukkit.getScheduler().runTaskLater(SRWorldBorder.getInstance(), () -> {
                attachment.remove();
                if (isSenderAvailable(sender))
                    sender.sendMessage(ChatColor.GREEN + target.getName() + " 的限时bypass权限已到期。");
                tempBypassTasks.remove(target.getUniqueId());
            }, timeInSeconds * 20L);

            tempBypassTasks.put(target.getUniqueId(), task);
        } else {
            // 取消限时bypass权限
            BukkitTask task = tempBypassTasks.remove(target.getUniqueId());
            if (task != null) {
                task.cancel();
            }
            target.addAttachment(SRWorldBorder.getInstance(), "srwb.bypass", false);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " 的限时bypass权限已被取消。");
        }

        return true;
    }

    private boolean isSenderAvailable(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).isOnline();
        }
        return true;
    }
}
