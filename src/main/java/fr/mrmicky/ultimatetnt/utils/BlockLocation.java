package fr.mrmicky.ultimatetnt.utils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * @author MrMicky
 */
public class BlockLocation {

    private final int x;
    private final int y;
    private final int z;
    private final String world;

    public BlockLocation(int x, int y, int z, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = Objects.requireNonNull(world, "world");
    }

    public BlockLocation(Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    public BlockLocation(Block block) {
        this(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getWorld() {
        return world;
    }

    public boolean isInChunk(Chunk chunk) {
        return x >> 4 == chunk.getX() && z >> 4 == chunk.getZ() && world.equals(chunk.getWorld().getName());
    }

    public boolean blockEquals(Block block) {
        return x == block.getX() && y == block.getY() && z == block.getZ() && world.equals(block.getWorld().getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockLocation that = (BlockLocation) o;
        return x == that.x &&
                y == that.y &&
                z == that.z &&
                world.equals(that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, world);
    }

    @Override
    public String toString() {
        return "BlockLocation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", world='" + world + '\'' +
                '}';
    }
}
