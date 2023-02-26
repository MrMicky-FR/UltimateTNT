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
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TNTListener implements Listener {

    private static final Material CRYING_OBSIDIAN = Material.getMaterial("CRYING_OBSIDIAN");

    private final Set<UUID> throwCooldown = new HashSet<>();

    private final Map<UUID, BlockLocation> fallingBlocks = new HashMap<>();
    private final Map<BlockLocation, BlockLocation> blocks = new HashMap<>();
    private final List<List<Block>> safeBlocks = new ArrayList<>();

    private final Map<BlockLocation, Integer> obsidianBlocks = new HashMap<>();

    private final UltimateTNT plugin;

    public TNTListener(UltimateTNT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        obsidianBlocks.remove(new BlockLocation(e.getBlock()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();

        if (block.getType() != Material.TNT) {
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
    public void onPlayerInteract(PlayerInteractEvent e) {
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
                    ItemStack handItem = player.getItemInHand();
                    if (handItem.getAmount() > 1) {
                        handItem.setAmount(handItem.getAmount() - 1);
                    } else {
                        handItem = null;
                    }

                    player.setItemInHand(handItem);
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
    public void onBlockDispense(BlockDispenseEvent e) {
        ItemStack item = e.getItem();

        if (item.getType() != Material.TNT || e.getBlock().getType() != Material.DISPENSER) {
            return;
        }

        Dispenser dispenser = (Dispenser) e.getBlock().getState();
        Inventory inv = dispenser.getInventory();
        BlockFace face = ((Directional) dispenser.getData()).getFacing();
        Block block = e.getBlock().getRelative(face);

        e.setCancelled(true);

        plugin.spawnTNT(block.getLocation(), null, plugin.getRandomTNTName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            int slot = inv.first(Material.TNT);

            if (slot < 0) {
                plugin.getLogger().warning("No TNT in BlockDispenseEvent");
                return;
            }

            ItemStack tntItem = inv.getItem(slot);
            if (tntItem.getAmount() > 1) {
                tntItem.setAmount(tntItem.getAmount() - 1);
                inv.setItem(slot, tntItem);
            } else {
                inv.clear(slot);
            }
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER
                || !plugin.isWorldEnabled(e.getEntity().getWorld())) {
            return;
        }

        if (e.getCause() == DamageCause.FALL) {
            e.setDamage(e.getDamage() / plugin.getConfig().getDouble("FallDamage"));
        } else if (e.getCause() == DamageCause.BLOCK_EXPLOSION) {
            e.setDamage(e.getDamage() / plugin.getConfig().getDouble("TNTDamage"));
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getEntityType() != EntityType.PLAYER || !isTNT(e.getDamager())) {
            return;
        }

        if (plugin.isWorldEnabled(e.getEntity().getWorld())) {
            e.setDamage(e.getDamage() / plugin.getConfig().getDouble("TNTDamage"));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplodeLow(EntityExplodeEvent e) {
        Entity entity = e.getEntity();

        if (!isTNT(entity) || !plugin.getConfig().getBoolean("ObsidianBreaker.Enable")) {
            return;
        }

        int radius = (int) (entity instanceof Explosive ? ((Explosive) entity).getYield() : 4);
        int amount = plugin.getConfig().getInt("ObsidianBreaker.Amount");

        Block block = e.getLocation().getBlock();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block relativeBlock = block.getRelative(x, y, z);

                    if (!isObsidian(relativeBlock.getType())) {
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
    public void onEntityExplode(EntityExplodeEvent e) {
        Entity entity = e.getEntity();

        if (!isTNT(entity) && !plugin.getConfig().getBoolean("AllExplosions")) {
            return;
        }

        if (!plugin.isWorldEnabled(entity.getWorld())) {
            return;
        }

        Entity source = entity instanceof TNTPrimed ? ((TNTPrimed) entity).getSource() : null;

        handleExplosion(e, source, e.getLocation(), e.getYield(), e.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!plugin.getConfig().getBoolean("AllExplosions")) {
            return;
        }

        if (!plugin.isWorldEnabled(e.getBlock().getWorld())) {
            return;
        }

        Location location = e.getBlock().getLocation();

        handleExplosion(e, null, location, e.getYield(), e.blockList());
    }

    private void handleExplosion(Event e, Entity source, Location location,
                                 float yield, List<Block> blockList) {
        boolean noBreak = plugin.getConfig().getBoolean("DisableBreak");
        boolean realistic = plugin.getConfig().getBoolean("RealisticExplosion");
        boolean restore = plugin.getConfig().getBoolean("RestoreBlocks.Enable");
        boolean whitelist = plugin.getConfig().getBoolean("Whitelist.Enable");
        List<String> blockBlacklist = plugin.getConfig().getStringList("BlacklistBlocks");
        List<String> blockWhitelist = plugin.getConfig().getStringList("Whitelist.BlockList");
        String name = plugin.getRandomTNTName();
        int maxFallingBlocks = plugin.getConfig().getInt("MaxFallingBlocksPerChunk") - getFallingBlocksInChunk(location.getChunk());

        if (plugin.getConfig().getBoolean("DisableDrops")) {
            yield = 0;

            if (e instanceof EntityExplodeEvent) {
                ((EntityExplodeEvent) e).setYield(0);
            } else {
                ((BlockExplodeEvent) e).setYield(0);
            }
        }

        Iterator<Block> blockIterator = blockList.iterator();
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
                if (!blocks.containsValue(new BlockLocation(block)) && isNotSafeBlock(block)) {
                    if (maxFallingBlocks > 0) {
                        double x = (Math.random() - Math.random()) / 1.5;
                        double y = Math.random();
                        double z = (Math.random() - Math.random()) / 1.5;

                        @SuppressWarnings("deprecation") // 1.8 compatibility
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

            if (!realistic && isObsidian(block.getType())) {
                if (yield > 0) {
                    block.breakNaturally();
                } else {
                    block.setType(Material.AIR);
                }
            }
        }

        if (realistic && restore) {
            // Blocks affected by the explosion should not be change
            safeBlocks.add(blockList);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    safeBlocks.remove(blockList), plugin.getConfig().getInt("RestoreBlocks.MaxDelay") + 60);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock)) {
            return;
        }

        BlockLocation location = fallingBlocks.remove(e.getEntity().getUniqueId());
        if (location != null) {
            blocks.put(location, new BlockLocation(e.getBlock()));
        }
    }

    private void restoreBlock(Block block, FallingBlock fallingBlock) {
        int min = plugin.getConfig().getInt("RestoreBlocks.MinDelay");
        int max = plugin.getConfig().getInt("RestoreBlocks.MaxDelay");
        if (fallingBlock != null) {
            fallingBlocks.put(fallingBlock.getUniqueId(), new BlockLocation(block));
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
                fallingBlocks.remove(fallingBlock.getUniqueId());

                // Remove blocks spawn by falling blocks
                BlockLocation location = blocks.remove(new BlockLocation(block.getLocation()));
                if (location != null) {
                    Block oldBlock = block.getWorld().getBlockAt(location.getX(), location.getY(), location.getZ());
                    if (isNotSafeBlock(oldBlock)) {
                        oldBlock.setType(Material.AIR);
                    }
                }
            }

            if (!plugin.containsIgnoreCase(plugin.getConfig().getStringList("RestoreBlocks.RestoreBlacklist"), state.getType().toString())) {
                state.update(true, false);
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
        return (int) Arrays.stream(chunk.getEntities())
                .filter(e -> e.getType() == EntityType.FALLING_BLOCK)
                .count();
    }

    private boolean isNotSafeBlock(Block block) {
        return safeBlocks.stream().noneMatch(list -> list.contains(block));
    }

    private boolean isObsidian(Material material) {
        return material == Material.OBSIDIAN || (CRYING_OBSIDIAN != null && material == CRYING_OBSIDIAN);
    }

    private boolean isTNT(Entity entity) {
        return entity.getType() == EntityType.PRIMED_TNT
                || entity.getType() == EntityType.MINECART_TNT
                || entity.getType() == EntityType.ENDER_CRYSTAL;
    }
}
