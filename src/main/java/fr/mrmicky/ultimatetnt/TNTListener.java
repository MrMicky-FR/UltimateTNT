package fr.mrmicky.ultimatetnt;

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
import org.bukkit.material.Directional;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TNTListener implements Listener {

    private final Set<UUID> throwCooldown = new HashSet<>();

    private final Map<FallingBlock, Location> fallingBlocks = new HashMap<>();
    private final Map<Location, Location> blocks = new HashMap<>();
    private final List<List<Block>> safeBlocks = new ArrayList<>();

    private final UltimateTNT plugin;

    public TNTListener(UltimateTNT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        if (p == null || b.getType() != Material.TNT) {
            return;
        }

        if (!plugin.getConfig().getBoolean("AutoIgnite") || !plugin.isWorldEnabled(b.getWorld())) {
            return;
        }

        b.setType(Material.AIR);
        plugin.spawnTNT(b.getLocation(), p, plugin.getRandomTNTName());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        Block b = e.getClickedBlock();

        if (item == null || !plugin.isWorldEnabled(p.getWorld())) {
            return;
        }

        if (plugin.getConfig().getBoolean("Throw.Enable") && p.getItemInHand().getType() == Material.TNT) {
            if (!(p.isSneaking() && plugin.getConfig().getBoolean("Throw.DisableOnSneak"))
                    && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                e.setCancelled(true);

                if (throwCooldown.contains(p.getUniqueId())) {
                    Bukkit.getScheduler().runTask(plugin, p::updateInventory);
                    return;
                }

                Vector velocity = p.getLocation().getDirection().multiply(plugin.getConfig().getDouble("Throw.Velocity"));
                plugin.spawnTNT(p.getEyeLocation(), p, plugin.getRandomTNTName(), false).setVelocity(velocity);
                throwCooldown.add(p.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> throwCooldown.remove(p.getUniqueId()), plugin.getConfig().getInt("Throw.Delay") * 20);

                if (p.getGameMode() != GameMode.CREATIVE) {
                    ItemStack item2 = p.getItemInHand();
                    if (item2.getAmount() > 1) {
                        item2.setAmount(item2.getAmount() - 1);
                    } else {
                        item2 = null;
                    }

                    p.setItemInHand(item2);
                }

                Sound sound = Sound.valueOf(Bukkit.getVersion().contains("1.8") ? "CHICKEN_EGG_POP" : "ENTITY_CHICKEN_EGG");
                p.playSound(p.getLocation(), sound, 1.0F, 1.0F);
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.isCancelled() && b.getType() == Material.TNT) {
            String typeName = item.getType().name(); // 1.13 support
            if (item.getType() != Material.FLINT_AND_STEEL && !typeName.equals("FIREBALL") && !typeName.equals("FIRE_CHARGE")) {
                return;
            }

            b.setType(Material.AIR);
            plugin.spawnTNT(b.getLocation(), p, plugin.getRandomTNTName());
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
        BlockFace face = ((Directional) d.getData()).getFacing();
        Block b = e.getBlock().getRelative(face);

        if (b.getType() != Material.AIR) {
            return;
        }

        plugin.spawnTNT(b.getLocation(), null, plugin.getRandomTNTName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            int slot = inv.first(Material.TNT);

            if (slot < 0) {
                plugin.getLogger().warning("No TNT in BlockDispenseEvent");
                return;
            }

            ItemStack item2 = inv.getItem(slot);
            if (item2.getAmount() > 1) {
                item2.setAmount(item2.getAmount() - 1);
                inv.setItem(slot, item2);
            } else {
                inv.clear(slot);
            }
        });
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER || e.getCause() != DamageCause.FALL) {
            return;
        }

        if (plugin.isWorldEnabled(e.getEntity().getWorld())) {
            e.setDamage(e.getDamage() / plugin.getConfig().getDouble("FallDamage"));
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntityType() != EntityType.PLAYER || e.getDamager().getType() != EntityType.PRIMED_TNT) {
            return;
        }

        if (plugin.isWorldEnabled(e.getEntity().getWorld())) {
            e.setDamage(e.getDamage() / plugin.getConfig().getDouble("TNTDamage"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.PRIMED_TNT && e.getEntityType() != EntityType.MINECART_TNT && !plugin.getConfig().getBoolean("AllExplosions")) {
            return;
        }

        Entity en = e.getEntity();
        if (!plugin.isWorldEnabled(en.getWorld())) {
            return;
        }

        Entity source = e.getEntityType() == EntityType.PRIMED_TNT ? ((TNTPrimed) en).getSource() : null;
        Iterator<Block> bs = e.blockList().iterator();

        boolean noBreak = plugin.getConfig().getBoolean("DisableBreak");
        boolean realistic = plugin.getConfig().getBoolean("RealisticExplosion");
        boolean restore = plugin.getConfig().getBoolean("RestoreBlocks.Enable");
        boolean whitelist = plugin.getConfig().getBoolean("Whitelist.Enable");
        List<String> blacklist = plugin.getConfig().getStringList("BlacklistBlocks");
        List<String> whitelists = plugin.getConfig().getStringList("Whitelist.BlockList");
        String name = plugin.getRandomTNTName();
        int maxFallingBlocks = plugin.getConfig().getInt("MaxFallingBlocksPerChunk")
                - getFallingBlocksInChunk(e.getLocation().getChunk());

        if (plugin.getConfig().getBoolean("DisableDrops")) {
            e.setYield(0.0F);
        }

        while (bs.hasNext()) {
            Block b = bs.next();

            if (b.getType() == Material.TNT) {
                b.setType(Material.AIR);
                plugin.spawnTNT(b.getLocation(), source, name);
                bs.remove();
            } else if (noBreak || plugin.containsIgnoreCase(blacklist, b.getType().toString())
                    || (whitelist && !plugin.containsIgnoreCase(whitelists, b.getType().toString()))) {
                bs.remove();
            } else if (realistic) {
                if (!blocks.containsValue(b.getLocation()) && !safeBlocksContains(b)) {
                    if (maxFallingBlocks > 0) {
                        double x = (Math.random() - Math.random()) / 1.5;
                        double y = Math.random();
                        double z = (Math.random() - Math.random()) / 1.5;

                        //noinspection deprecation
                        FallingBlock fall = b.getWorld().spawnFallingBlock(b.getLocation(), b.getType(), b.getData());
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
            List<Block> list = new ArrayList<>(e.blockList());
            safeBlocks.add(list);
            Bukkit.getScheduler().runTaskLater(plugin, () -> safeBlocks.remove(list), plugin.getConfig().getInt("RestoreBlocks.MaxDelay") + 60);
        }
    }

    @EventHandler
    public void onBlockChange(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock)) {
            return;
        }

        Location loc = fallingBlocks.remove(e.getEntity());
        if (loc != null) {
            blocks.put(loc, e.getBlock().getLocation());
        }
    }

    private void restoreBlock(Block b, FallingBlock fall) {
        int min = plugin.getConfig().getInt("RestoreBlocks.MinDelay");
        int max = plugin.getConfig().getInt("RestoreBlocks.MaxDelay");
        if (fall != null) {
            fallingBlocks.put(fall, b.getLocation());
        }

        BlockState bs = b.getState();
        String[] signLines = bs instanceof Sign ? ((Sign) bs).getLines() : null;
        ItemStack[] items = bs instanceof InventoryHolder ? ((InventoryHolder) bs).getInventory().getContents() : null;

        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    items[i] = items[i].clone();
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fall != null) {
                if (fall.isValid()) {
                    fall.remove();
                }
                fallingBlocks.remove(fall);

                // Remove blocks spawn by falling blocks
                Location loc = blocks.remove(b.getLocation());
                if (loc != null) {
                    Block b2 = loc.getBlock();
                    if (!safeBlocksContains(b2)) {
                        b2.setType(Material.AIR);
                    }
                }
            }

            if (!plugin.containsIgnoreCase(plugin.getConfig().getStringList("RestoreBlocks.RestoreBlacklist"), bs.getType().toString())) {
                bs.update(true);
                BlockState bs2 = b.getState();
                // restore inventory contents & signs lines
                if (signLines != null && bs2 instanceof Sign) {
                    Sign s = (Sign) bs2;
                    for (int i = 0; i < 4; i++) {
                        s.setLine(i, signLines[i]);
                    }
                    s.update();
                } else if (items != null && bs2 instanceof InventoryHolder) {
                    ((InventoryHolder) bs2).getInventory().setContents(items);
                }
            }
        }, min + UltimateTNT.RANDOM.nextInt(max - min));
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
        return safeBlocks.stream().anyMatch(list -> list.contains(b));
    }
}
