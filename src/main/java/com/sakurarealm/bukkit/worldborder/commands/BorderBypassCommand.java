package com.sakurarealm.bukkit.worldborder.commands;

import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BorderBypassCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("srwb.admin")) {
                sender.sendMessage(ChatColor.DARK_RED + "你没有权限执行此命令。");
                return true;
            }
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.DARK_RED + "使用方法：/borderbypass <玩家名> <true|false>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.DARK_RED + "指定的玩家不在线。");
            return true;
        }

        boolean enableBypass = args[1].equalsIgnoreCase("true");

        if (enableBypass) {
            target.addAttachment(SRWorldBorder.getInstance(), "srwb.bypass", true);
            sender.sendMessage(target.getName() + " 现在拥有bypass权限。");
        } else {
            // 取消现时的bypass权限
            target.addAttachment(SRWorldBorder.getInstance(), "srwb.bypass", false);
            sender.sendMessage(target.getName() + " 的bypass权限已被移除。");
        }

        return true;
    }
}
