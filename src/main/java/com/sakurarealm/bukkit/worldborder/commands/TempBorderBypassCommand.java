package com.sakurarealm.bukkit.worldborder.commands;

import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import com.sakurarealm.bukkit.worldborder.utils.BypassPermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempBorderBypassCommand implements CommandExecutor {

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.DARK_RED + "使用方法：/" + label + " <玩家名> <时间(秒)>\n" +
                ChatColor.DARK_RED + "Deprecated：/" + label + " <玩家名> true <时间(秒)>\n" +
                ChatColor.DARK_RED + "Deprecated：/" + label + " <玩家名> false");
    }

    private void args2Command(CommandSender sender, Command command, String label, String[] args) {
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.DARK_RED + "指定的玩家不在线。");
            return;
        }

        // 如果第二个参数是true或false，那么就是旧的用法
        BypassPermissionManager bypassPermissionManager = SRWorldBorder.getInstance().getBypassPermissionManager();
        if (args[1].equalsIgnoreCase("false")) {

            boolean success = bypassPermissionManager.removePlayerBypassPermission(target, true);

            if (success) {
                sender.sendMessage(ChatColor.GREEN + target.getName() + "已经移除限时权限");
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "无法为玩家移除限时权限: 玩家 "
                        + target.getName() + " 拥有永久bypass权限");
            }
            return;
        }

        // 如果第二个参数不是true或false，那么就是新的用法
        String timeString = args[1];
        givePermission(sender, label, target, timeString);
    }

    private void args3Command(CommandSender sender, Command command, String label, String[] args) {
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.DARK_RED + "指定的玩家不在线。");
            return;
        } else if (!args[1].equalsIgnoreCase("true")) { // 第二个参数不是true
            sendUsage(sender, label);
            return;
        }

        String timeString = args[2];
        givePermission(sender, label, target, timeString);
    }

    private void givePermission(CommandSender sender, String commandLabel, Player player, String timeString) {
        int timeInSeconds;
        try {
            timeInSeconds = Integer.parseInt(timeString);
            assert timeInSeconds > 0;
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.DARK_RED + "无效的时间: " + timeString);
            sendUsage(sender, commandLabel);
            return;
        }
        BypassPermissionManager bypassPermissionManager = SRWorldBorder.getInstance().getBypassPermissionManager();
        boolean success = bypassPermissionManager.givePlayerBypassPermission(player, timeInSeconds);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + player.getName() +
                    " 现在拥有限时bypass限时权限，持续 " + timeInSeconds + " 秒。");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "无法为玩家添加bypass限时权限: 玩家 "
                    + player.getName() + " 已经拥有了永久bypass权限");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("srwb.admin")) {
                sender.sendMessage(ChatColor.DARK_RED + "你没有权限执行此命令。");
                return true;
            }
        }

        if (args.length == 2) {
            args2Command(sender, command, label, args);
            return true;
        } else if (args.length == 3) {
            args3Command(sender, command, label, args);
            return true;
        } else {
            sendUsage(sender, label);
            return true;
        }

    }

}
