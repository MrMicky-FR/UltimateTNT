package fr.mrmicky.ultimatetnt;

import fr.mrmicky.ultimatetnt.utils.BlockLocation;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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

    private final Map<BlockLocation, Integer> obsidianBlocks = new HashMap<>();

    private final UltimateTNT plugin;

    public TNTListener(UltimateTNT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        obsidianBlocks.remove(new BlockLocation(e.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();

        if (player == null || block.getType() != Material.TNT) {
            return;
        }

        if (!plugin.getConfig().getBoolean("AutoIgnite") || !plugin.isWorldEnabled(block.getWorld())) {
            return;
        }

        block.setType(Material.AIR);
        plugin.spawnTNT(block.getLocation(), player, plugin.getRandomTNTName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        obsidianBlocks.keySet().removeIf(loc -> loc.isInChunk(e.getChunk()));
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        Block block = e.getClickedBlock();

        if (item == null || !plugin.isWorldEnabled(player.getWorld())) {
            return;
        }

        if (plugin.getConfig().getBoolean("Throw.Enable") && player.getItemInHand().getType() == Material.TNT) {
            if (!(player.isSneaking() && plugin.getConfig().getBoolean("Throw.DisableOnSneak"))
                    && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                e.setCancelled(true);

                if (throwCooldown.contains(player.getUniqueId())) {
                    Bukkit.getScheduler().runTask(plugin, player::updateInventory);
                    return;
                }

                Vector velocity = player.getLocation().getDirection().multiply(plugin.getConfig().getDouble("Throw.Velocity"));
                plugin.spawnTNT(player.getEyeLocation(), player, plugin.getRandomTNTName(), false).setVelocity(velocity);
                throwCooldown.add(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> throwCooldown.remove(player.getUniqueId()), plugin.getConfig().getInt("Throw.Delay") * 20);

                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemStack item2 = player.getItemInHand();
                    if (item2.getAmount() > 1) {
                        item2.setAmount(item2.getAmount() - 1);
                    } else {
                        item2 = null;
                    }

                    player.setItemInHand(item2);
                }

                Sound sound = Sound.valueOf(Bukkit.getVersion().contains("1.8") ? "CHICKEN_EGG_POP" : "ENTITY_CHICKEN_EGG");
                player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.isCancelled() && block != null && block.getType() == Material.TNT) {
            String typeName = item.getType().name(); // 1.13 support
            if (item.getType() != Material.FLINT_AND_STEEL && !typeName.equals("FIREBALL") && !typeName.equals("FIRE_CHARGE")) {
                return;
            }

            block.setType(Material.AIR);
            plugin.spawnTNT(block.getLocation(), player, plugin.getRandomTNTName());
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

        Dispenser dispenser = (Dispenser) e.getBlock().getState();
        Inventory inv = dispenser.getInventory();
        BlockFace face = ((Directional) dispenser.getData()).getFacing();
        Block block = e.getBlock().getRelative(face);

        if (block.getType() != Material.AIR) {
            return;
        }

        plugin.spawnTNT(block.getLocation(), null, plugin.getRandomTNTName());

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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplodeObsidian(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.PRIMED_TNT || !plugin.getConfig().getBoolean("ObsidianBreaker.Enable")) {
            return;
        }

        int radius = (int) ((TNTPrimed) e.getEntity()).getYield();
        int amount = plugin.getConfig().getInt("ObsidianBreaker.Amount");

        Block block = e.getLocation().getBlock();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relativeBlock = block.getRelative(x, y, z);

                    if (relativeBlock.getType() != Material.OBSIDIAN) {
                        continue;
                    }

                    BlockLocation blockLocation = new BlockLocation(relativeBlock);

                    int explosions = obsidianBlocks.getOrDefault(blockLocation, 0) + 1;

                    if (explosions < amount) {
                        obsidianBlocks.put(blockLocation, explosions);
                    } else {
                        obsidianBlocks.remove(blockLocation);

                        e.blockList().add(relativeBlock);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (e.getEntityType() != EntityType.PRIMED_TNT && e.getEntityType() != EntityType.MINECART_TNT && !plugin.getConfig().getBoolean("AllExplosions")) {
            return;
        }

        Entity entity = e.getEntity();
        if (!plugin.isWorldEnabled(entity.getWorld())) {
            return;
        }

        Entity source = e.getEntityType() == EntityType.PRIMED_TNT ? ((TNTPrimed) entity).getSource() : null;

        boolean noBreak = plugin.getConfig().getBoolean("DisableBreak");
        boolean realistic = plugin.getConfig().getBoolean("RealisticExplosion");
        boolean restore = plugin.getConfig().getBoolean("RestoreBlocks.Enable");
        boolean whitelist = plugin.getConfig().getBoolean("Whitelist.Enable");
        List<String> blockBlacklist = plugin.getConfig().getStringList("BlacklistBlocks");
        List<String> blockWhitelist = plugin.getConfig().getStringList("Whitelist.BlockList");
        String name = plugin.getRandomTNTName();
        int maxFallingBlocks = plugin.getConfig().getInt("MaxFallingBlocksPerChunk") - getFallingBlocksInChunk(e.getLocation().getChunk());

        if (plugin.getConfig().getBoolean("DisableDrops")) {
            e.setYield(0.0F);
        }

        Iterator<Block> blockIterator = e.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();

            if (block.getType() == Material.TNT) {
                block.setType(Material.AIR);
                plugin.spawnTNT(block.getLocation(), source, name);
                blockIterator.remove();
            } else if (noBreak || plugin.containsIgnoreCase(blockBlacklist, block.getType().toString())
                    || (whitelist && !plugin.containsIgnoreCase(blockWhitelist, block.getType().toString()))) {
                blockIterator.remove();
            } else if (realistic) {
                if (!blocks.containsValue(block.getLocation()) && !safeBlocksContains(block)) {
                    if (maxFallingBlocks > 0) {
                        double x = (Math.random() - Math.random()) / 1.5;
                        double y = Math.random();
                        double z = (Math.random() - Math.random()) / 1.5;

                        //noinspection deprecation
                        FallingBlock fall = block.getWorld().spawnFallingBlock(block.getLocation(), block.getType(), block.getData());
                        fall.setDropItem(false);
                        fall.setVelocity(new Vector(x, y, z));
                        maxFallingBlocks--;
                        if (restore) {
                            restoreBlock(block, fall);
                        }
                    } else if (restore) {
                        restoreBlock(block, null);
                    }
                    block.setType(Material.AIR);
                }
            } else if (restore && block.getType() != Material.AIR) {
                restoreBlock(block, null);
            }

            if (!realistic && block.getType() == Material.OBSIDIAN) {
                if (e.getYield() > 0) {
                    block.breakNaturally();
                } else {
                    block.setType(Material.AIR);
                }
            }
        }

        if (realistic && restore) {
            // Blocks affected by the explosion should not be change
            List<Block> blocks = new ArrayList<>(e.blockList());
            safeBlocks.add(blocks);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    safeBlocks.remove(blocks), plugin.getConfig().getInt("RestoreBlocks.MaxDelay") + 60);
        }
    }

    @EventHandler
    public void onBlockChange(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock)) {
            return;
        }

        Location location = fallingBlocks.remove(e.getEntity());
        if (location != null) {
            blocks.put(location, e.getBlock().getLocation());
        }
    }

    private void restoreBlock(Block block, FallingBlock fallingBlock) {
        int min = plugin.getConfig().getInt("RestoreBlocks.MinDelay");
        int max = plugin.getConfig().getInt("RestoreBlocks.MaxDelay");
        if (fallingBlock != null) {
            fallingBlocks.put(fallingBlock, block.getLocation());
        }

        BlockState state = block.getState();
        String[] signLines = state instanceof Sign ? ((Sign) state).getLines() : null;
        ItemStack[] items = state instanceof InventoryHolder ? ((InventoryHolder) state).getInventory().getContents() : null;

        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    items[i] = items[i].clone();
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (fallingBlock != null) {
                if (fallingBlock.isValid()) {
                    fallingBlock.remove();
                }
                fallingBlocks.remove(fallingBlock);

                // Remove blocks spawn by falling blocks
                Location location = blocks.remove(block.getLocation());
                if (location != null) {
                    Block block2 = location.getBlock();
                    if (!safeBlocksContains(block2)) {
                        block2.setType(Material.AIR);
                    }
                }
            }

            if (!plugin.containsIgnoreCase(plugin.getConfig().getStringList("RestoreBlocks.RestoreBlacklist"), state.getType().toString())) {
                state.update(true);
                BlockState newState = block.getState();
                // restore inventory contents & signs lines
                if (signLines != null && newState instanceof Sign) {
                    Sign sign = (Sign) newState;
                    for (int i = 0; i < 4; i++) {
                        sign.setLine(i, signLines[i]);
                    }
                    sign.update();
                } else if (items != null && newState instanceof InventoryHolder) {
                    ((InventoryHolder) newState).getInventory().setContents(items);
                }
            }
        }, min + UltimateTNT.RANDOM.nextInt(max - min));
    }

    private int getFallingBlocksInChunk(Chunk chunk) {
        int fallingBlocksCount = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.FALLING_BLOCK) {
                fallingBlocksCount++;
            }
        }
        return fallingBlocksCount;
    }

    private boolean safeBlocksContains(Block b) {
        return safeBlocks.stream().anyMatch(list -> list.contains(b));
    }
}
