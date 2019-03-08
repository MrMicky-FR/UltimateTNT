package fr.mrmicky.ultimatetnt;

import fr.mrmicky.ultimatetnt.utils.ChatUtils;
import fr.mrmicky.ultimatetnt.utils.TntUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

public class UltimateTNT extends JavaPlugin {

    public static final Random RANDOM = new Random();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getCommand("ultimatetnt").setExecutor(new CommandUltimateTNT(this));
        getServer().getPluginManager().registerEvents(new TNTListener(this), this);

        if (getConfig().getBoolean("UpdateChecker")) {
            getServer().getScheduler().runTaskAsynchronously(this, this::checkUpdate);
        }
    }

    private void checkUpdate() {
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=49388");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String lastVersion = reader.readLine();
                if (!getDescription().getVersion().equalsIgnoreCase(lastVersion)) {
                    getLogger().warning("A new version is available ! Last version is " + lastVersion + " and you are on " + getDescription().getVersion());
                    getLogger().warning("You can download it on: " + getDescription().getWebsite());
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isWorldEnabled(World w) {
        return !containsIgnoreCase(getConfig().getStringList("DisableWorlds"), w.getName());
    }

    public String getRandomTNTName() {
        List<String> names = getConfig().getStringList("Names");
        return ChatUtils.color(names.get(RANDOM.nextInt(names.size())));
    }

    public TNTPrimed spawnTNT(Block b, Player p, String tntName) {
        b.setType(Material.AIR);
        Location loc = b.getLocation().add(0.5, 0.25, 0.5);
        TNTPrimed tnt = b.getWorld().spawn(loc, TNTPrimed.class);

        tnt.setVelocity(new Vector(0, 0.25, 0));
        tnt.teleport(loc);
        tnt.setIsIncendiary(getConfig().getBoolean("Fire"));
        tnt.setFuseTicks(getConfig().getInt("ExplodeTicks"));
        tnt.setYield((float) getConfig().getDouble("Radius"));

        if (getConfig().getBoolean("CustomName")) {
            tnt.setCustomNameVisible(true);

            if (!tntName.contains("%timer")) {
                tnt.setCustomName(tntName);
            } else {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        tnt.setCustomName(tntName.replace("%timer", DECIMAL_FORMAT.format(tnt.getFuseTicks() / 20.0)));

                        if (!tnt.isValid() || tnt.getFuseTicks() <= 0) {
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 0, 1);
            }
        }

        if (p != null) {
            try {
                TntUtils.setTntSource(tnt, p);
            } catch (ReflectiveOperationException e) {
                getLogger().warning("Cannot set the source for " + tnt + ": " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
        return tnt;
    }

    public boolean containsIgnoreCase(List<String> list, String s) {
        for (String l : list) {
            if (l.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
