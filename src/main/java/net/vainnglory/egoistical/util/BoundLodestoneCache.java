package net.vainnglory.egoistical.util;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BoundLodestoneCache {

    private static final Map<BlockPos, String> boundLodestones = new HashMap<>();

    public static void add(BlockPos pos, String dimension) {
        boundLodestones.put(pos.toImmutable(), dimension);
    }

    public static void remove(BlockPos pos) {
        boundLodestones.remove(pos);
    }

    public static Map<BlockPos, String> getAll() {
        return Collections.unmodifiableMap(boundLodestones);
    }
}

