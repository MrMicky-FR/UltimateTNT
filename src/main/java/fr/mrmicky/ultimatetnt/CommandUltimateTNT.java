package fr.mrmicky.ultimatetnt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.Collections;
import java.util.List;

public class CommandUltimateTNT implements TabExecutor {

    private UltimateTNT m;

    CommandUltimateTNT(UltimateTNT m) {
        this.m = m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || !sender.hasPermission("ultimatetnt.reload")) {
            sender.sendMessage("§c" + m.getName() + "§6 by §cMrMicky §6version §c" + m.getDescription().getVersion());
            sender.sendMessage("§6Download: §c" + m.getDescription().getWebsite());
        } else if (args[0].equalsIgnoreCase("reload")) {
            m.reloadConfig();
            sender.sendMessage("§aConfig reloaded");
        } else {
            sender.sendMessage("§c/ultimatetnt reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 0 && sender.hasPermission("ultimatetnt.reload")) {
            if (StringUtil.startsWithIgnoreCase("reload", args[1])) {
                return Collections.singletonList("reload");
            }
        }
        return Collections.emptyList();
    }
}
