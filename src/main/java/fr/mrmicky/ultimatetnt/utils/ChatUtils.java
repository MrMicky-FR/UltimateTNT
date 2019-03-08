package fr.mrmicky.ultimatetnt.utils;

import org.bukkit.ChatColor;

/**
 * @author MrMicky
 */
public final class ChatUtils {

    private ChatUtils() {
        throw new UnsupportedOperationException();
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
