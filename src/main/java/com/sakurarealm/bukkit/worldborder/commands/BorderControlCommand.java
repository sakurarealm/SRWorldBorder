package com.sakurarealm.bukkit.worldborder.commands;

import com.sakurarealm.bukkit.worldborder.SRWorldBorder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class BorderControlCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (!sender.hasPermission("srwb.admin")) {
                sender.sendMessage(ChatColor.DARK_RED + "你没有权限执行此命令。");
                return true;
            }
        }
        if (args.length < 1)
            sender.sendMessage(ChatColor.DARK_RED + "使用方法：/bordercontrol <reload|info>");

        if (args[0].equalsIgnoreCase("reload"))
            reload(sender);
        else if (args[0].equalsIgnoreCase("info"))
            info(sender);
        else
            sender.sendMessage(ChatColor.DARK_RED + "使用方法：/bordercontrol <reload|info>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    private void reload(CommandSender sender) {
        SRWorldBorder plugin = SRWorldBorder.getInstance();
        if (plugin == null)
            return;

        plugin.loadBorderConfig();
        sender.sendMessage(ChatColor.GREEN + "已重载服务器边界配置! 新的配置:\n" +
                SRWorldBorder.getInstance().getBorderConfig().getBorderInfo());
    }

    private void info(CommandSender sender) {
        SRWorldBorder plugin = SRWorldBorder.getInstance();
        if (plugin == null)
            return;

        plugin.loadBorderConfig();
        sender.sendMessage(SRWorldBorder.getInstance().getBorderConfig().getBorderInfo());
    }
}
