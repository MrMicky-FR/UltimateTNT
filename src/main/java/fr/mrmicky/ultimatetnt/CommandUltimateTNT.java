package fr.mrmicky.ultimatetnt;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.Collections;
import java.util.List;

public class CommandUltimateTNT implements TabExecutor {

    private final UltimateTNT plugin;

    public CommandUltimateTNT(UltimateTNT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("ultimatetnt.reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.YELLOW + "Config reloaded");
            return true;
        }

        sendUsage(sender);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("ultimatetnt.reload")) {
            if (StringUtil.startsWithIgnoreCase("reload", args[0])) {
                return Collections.singletonList("reload");
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.color("&cUltimateTNT v" + plugin.getDescription().getVersion() + " &7by &cMrMicky&7."));

        if (sender.hasPermission("ultimatetnt.reload")) {
            sender.sendMessage(plugin.color("&7- &c/ultimatetnt reload"));
        }
    }
}
