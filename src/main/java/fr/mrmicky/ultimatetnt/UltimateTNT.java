package fr.mrmicky.ultimatetnt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class UltimateTNT extends JavaPlugin {

	Random r = new Random();
	// Reflection
	private Method playerHandleMethod;
	private Method tntHandleMethod;
	private Field tntSourceField;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		getCommand("ultimatetnt").setExecutor(new CommandUltimateTNT(this));
		getServer().getPluginManager().registerEvents(new TNTListener(this), this);

		if (getConfig().getBoolean("UpdateChecker")) {
			// Delay so the message have better visibility
			getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(), 60);
		}

		getLogger().info("The plugin has been successfully activated");
	}

	private void checkUpdate() {
		try {
			URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=49388");
			String lastVersion = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))
					.readLine();
			if (!getDescription().getVersion().equalsIgnoreCase(lastVersion)) {
				getLogger().info("A new version is avaible ! Last version is " + lastVersion + " and you are on "
						+ getDescription().getVersion());
				getLogger().info("You can download it on: " + getDescription().getWebsite());
			}
		} catch (Exception e) {
			getLogger().warning("Failed to check for update on SpigotMC:");
			e.printStackTrace();
		}
	}

	public boolean isWorldEnabled(World w) {
		return !containsIgnoreCase(getConfig().getStringList("DisableWorlds"), w.getName());
	}

	public String getRandomTNTName() {
		List<String> names = getConfig().getStringList("Names");
		return names.get(r.nextInt(names.size())).replace('&', '§');
	}

	public TNTPrimed spawnTNT(Block b, Player p, String tntName) {
		b.setType(Material.AIR);
		Location loc = b.getLocation().add(0.5, 0.25, 0.5);
		TNTPrimed tnt = (TNTPrimed) b.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);

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
				DecimalFormat df = new DecimalFormat("0.0");
				new BukkitRunnable() {

					@Override
					public void run() {
						tnt.setCustomName(tntName.replace("%timer", df.format(tnt.getFuseTicks() / 20.0D)));

						if (!tnt.isValid() || tnt.getFuseTicks() <= 0) {
							cancel();
						}
					}
				}.runTaskTimer(this, 0, 1);
			}
		}

		if (p != null) {
			try {
				if (playerHandleMethod == null) {
					playerHandleMethod = p.getClass().getDeclaredMethod("getHandle");
					tntHandleMethod = tnt.getClass().getDeclaredMethod("getHandle");
					tntSourceField = tntHandleMethod.getReturnType().getDeclaredField("source");
					tntSourceField.setAccessible(true);
				}
				Object craftTNT = tntHandleMethod.invoke(tnt);
				tntSourceField.set(craftTNT, playerHandleMethod.invoke(p));
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
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
