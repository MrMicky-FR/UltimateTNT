package fr.mrmicky.ultimatetnt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.util.Vector;

public class TNTListener implements Listener {

	private UltimateTNT m;

	private Map<FallingBlock, Location> fallingBlocks = new HashMap<>();
	private Map<Location, Location> blocks = new HashMap<>();
	private List<List<Block>> safeBlocks = new ArrayList<>();

	private List<UUID> throwCooldown = new ArrayList<>();

	public TNTListener(UltimateTNT m) {
		this.m = m;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		Block b = e.getBlock();

		if (p == null || b == null) {
			return;
		}

		if (b.getType() == Material.TNT && m.getConfig().getBoolean("AutoIgnite") && m.isWorldEnabled(b.getWorld())) {
			b.setType(Material.AIR);
			m.spawnTNT(b, p, m.getRandomTNTName());
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		ItemStack item = e.getItem();
		Block b = e.getClickedBlock();

		if (item == null || !m.isWorldEnabled(p.getWorld())) {
			return;
		}

		if (m.getConfig().getBoolean("Throw.Enable") && p.getItemInHand().getType() == Material.TNT) {
			if (!(p.isSneaking() && m.getConfig().getBoolean("Throw.DisableOnSneak"))
					&& (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
				e.setCancelled(true);

				if (throwCooldown.contains(p.getUniqueId())) {
					Bukkit.getScheduler().runTask(m, p::updateInventory);
					return;
				}

				m.spawnTNT(p.getEyeLocation().getBlock(), p, m.getRandomTNTName()).setVelocity(
						p.getLocation().getDirection().multiply(m.getConfig().getDouble("Throw.Velocity")));
				throwCooldown.add(p.getUniqueId());
				Bukkit.getScheduler().runTaskLater(m, () -> throwCooldown.remove(p.getUniqueId()),
						m.getConfig().getInt("Throw.Delay") * 20);

				if (p.getGameMode() != GameMode.CREATIVE) {
					ItemStack item2 = p.getItemInHand();
					item2.setAmount(item2.getAmount() - 1);
					p.setItemInHand(item2);
				}

				p.playSound(p.getLocation(), Sound.valueOf(Bukkit.getServer().getClass().getName().contains("1.8")
						? "CHICKEN_EGG_POP" : "ENTITY_CHICKEN_EGG"), 1.0F, 1.0F);
			}
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.isCancelled() && b.getType() == Material.TNT
				&& (item.getType() == Material.FLINT_AND_STEEL || item.getType() == Material.FIREBALL)) {
			m.spawnTNT(b, p, m.getRandomTNTName());
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDispense(BlockDispenseEvent e) {
		ItemStack item = e.getItem();
		if (item == null || item.getType() != Material.TNT || e.getBlock().getType() != Material.DISPENSER) {
			return;
		}

		e.setCancelled(true);

		Dispenser d = (Dispenser) e.getBlock().getState();
		Inventory inv = d.getInventory();
		BlockFace face = ((DirectionalContainer) d.getData()).getFacing();
		Block b = e.getBlock().getRelative(face);

		if (b.getType() == Material.AIR) {
			m.spawnTNT(b, null, m.getRandomTNTName());
			ItemStack item2 = inv.getItem(inv.first(Material.TNT));
			item2.setAmount(item2.getAmount() - 1);
			inv.setItem(inv.first(Material.TNT), item2);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntityType() == EntityType.PLAYER && e.getCause() == DamageCause.FALL
				&& m.isWorldEnabled(e.getEntity().getWorld())) {
			e.setDamage(e.getDamage() / m.getConfig().getDouble("FallDamage"));
		}
	}

	@EventHandler
	public void onDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntityType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PRIMED_TNT
				&& m.isWorldEnabled(e.getEntity().getWorld())) {
			e.setDamage(e.getDamage() / m.getConfig().getDouble("TNTDamage"));
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onExplode(EntityExplodeEvent e) {
		Entity en = e.getEntity();
		if ((e.getEntityType() == EntityType.PRIMED_TNT || e.getEntityType() == EntityType.MINECART_TNT
				|| m.getConfig().getBoolean("AllExplosions")) && m.isWorldEnabled(en.getWorld())) {
			Entity source = e.getEntityType() == EntityType.PRIMED_TNT ? ((TNTPrimed) en).getSource() : null;
			Iterator<Block> bs = e.blockList().iterator();

			boolean noBreak = m.getConfig().getBoolean("DisableBreak");
			boolean realistic = m.getConfig().getBoolean("RealisticExplosion");
			boolean restore = m.getConfig().getBoolean("RestoreBlocks.Enable");
			boolean whitelist = m.getConfig().getBoolean("Whitelist.Enable");
			List<String> blacklist = m.getConfig().getStringList("BlacklistBlocks");
			List<String> whitelists = m.getConfig().getStringList("Whitelist.BlockList");
			String name = m.getRandomTNTName();
			int maxFallingBlocks = m.getConfig().getInt("MaxFallingBlocksPerChunk")
					- getFallingBlocksInChunk(e.getLocation().getChunk());

			if (m.getConfig().getBoolean("DisableDrops")) {
				e.setYield(0.0F);
			}

			while (bs.hasNext()) {
				Block b = bs.next();
				if (b.getType() == Material.TNT) {
					m.spawnTNT(b, source != null && source.getType() == EntityType.PLAYER ? (Player) source : null,
							name);
					bs.remove();
				} else if (noBreak || m.containsIgnoreCase(blacklist, b.getType().toString())
						|| (whitelist && !m.containsIgnoreCase(whitelists, b.getType().toString()))) {
					bs.remove();
				} else if (realistic) {
					if (!blocks.containsValue(b.getLocation()) && !safeBlocksContains(b)) {
						if (maxFallingBlocks > 0) {
							double x = (Math.random() - Math.random()) / 1.5;
							double y = Math.random();
							double z = (Math.random() - Math.random()) / 1.5;

							// Deprecated but no other way to do that in 1.8
							FallingBlock fall = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(),
									b.getData());
							fall.setDropItem(false);
							fall.setVelocity(new Vector(x, y, z));
							maxFallingBlocks--;
							if (restore) {
								restoreBlock(b, fall);
							}
						} else if (restore) {
							restoreBlock(b, null);
						}
						b.setType(Material.AIR);
					}
				} else if (restore && b.getType() != Material.AIR) {
					restoreBlock(b, null);
				}
			}

			if (realistic && restore) {
				// Blocks affected by the explosion should not be change
				List<Block> list = e.blockList();
				safeBlocks.add(list);
				Bukkit.getScheduler().runTaskLater(m, () -> safeBlocks.remove(list),
						m.getConfig().getInt("RestoreBlocks.MaxDelay") + 60);
			}
		}
	}

	@EventHandler
	public void onBlockChange(EntityChangeBlockEvent e) {
		if (e.getEntityType() == EntityType.FALLING_BLOCK && fallingBlocks.containsKey(e.getEntity())) {
			blocks.put(fallingBlocks.get(e.getEntity()), e.getBlock().getLocation());
			fallingBlocks.remove(e.getEntity());
		}
	}

	private void restoreBlock(Block b, FallingBlock fall) {
		int min = m.getConfig().getInt("RestoreBlocks.MinDelay");
		int max = m.getConfig().getInt("RestoreBlocks.MaxDelay");
		if (fall != null) {
			fallingBlocks.put(fall, b.getLocation());
		}

		final BlockState bs = b.getState();
		String[] signLines = bs instanceof Sign ? ((Sign) bs).getLines() : null;
		ItemStack[] items = bs instanceof InventoryHolder ? ((InventoryHolder) bs).getInventory().getContents() : null;

		if (items != null) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] != null) {
					items[i] = items[i].clone();
				}
			}
		}

		Bukkit.getScheduler().runTaskLater(m, () -> {
			if (fall != null) {
				if (fall.isValid()) {
					fall.remove();
				}
				fallingBlocks.remove(fall);

				// Remove blocks spawn by falling blocks
				if (blocks.containsKey(b.getLocation())) {
					Location loc = blocks.get(b.getLocation());
					blocks.remove(b.getLocation());
					Block b2 = loc.getBlock();
					if (!safeBlocksContains(b2)) {
						b2.setType(Material.AIR);
					}
				}
			}

			if (!m.containsIgnoreCase(m.getConfig().getStringList("RestoreBlocks.RestoreBlacklist"),
					bs.getType().toString())) {
				bs.update(true);
				BlockState bs2 = b.getState();
				// restore inventory contents & signs lines
				if (signLines != null && bs2 instanceof Sign) {
					Sign s = (Sign) bs2;
					for (int i = 0; i < 4; i++) {
						s.setLine(i, signLines[i]);
					}
				} else if (items != null && bs2 instanceof InventoryHolder) {
					((InventoryHolder) bs2).getInventory().setContents(items);
				}
			}
		}, min + m.r.nextInt(max - min));
	}

	private int getFallingBlocksInChunk(Chunk c) {
		int falling = 0;
		for (Entity e : c.getEntities()) {
			if (e.getType() == EntityType.FALLING_BLOCK) {
				falling++;
			}
		}
		return falling;
	}

	private boolean safeBlocksContains(Block b) {
		for (List<Block> list : safeBlocks) {
			if (list.contains(b)) {
				return true;
			}
		}
		return false;
	}
}