package fr.mrmicky.ultimatetnt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandUltimateTNT implements CommandExecutor {

    private UltimateTNT m;

    CommandUltimateTNT(UltimateTNT m) {
        this.m = m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
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
}
