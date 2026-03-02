package net.vainnglory.egoistical.util;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PendingVoidTeleport {

    private record Entry(long scheduledAt, long teleportAt, double x, double z, String dimensionId) {}

    private static final Map<UUID, Entry> pending = new HashMap<>();

    private static final int DELAY = 100; // 5 seconds

    public static void schedule(ServerPlayerEntity player, long currentTime, double x, double z, String dimensionId) {
        pending.put(player.getUuid(), new Entry(currentTime, currentTime + DELAY, x, z, dimensionId));
    }

    public static void tick(MinecraftServer server, long currentTime) {
        Iterator<Map.Entry<UUID, Entry>> it = pending.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Entry> e = it.next();
            Entry entry = e.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            long elapsed = currentTime - entry.scheduledAt();
            long remaining = entry.teleportAt() - currentTime;

            // Teleport time reached — make sure they're visible again then void them
            if (remaining <= 0) {
                it.remove();
                player.setInvisible(false);

                RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, new Identifier(entry.dimensionId()));
                ServerWorld targetWorld = server.getWorld(dimKey);
                if (targetWorld == null) continue;

                player.teleport(targetWorld, entry.x(), -100, entry.z(), player.getYaw(), player.getPitch());
                continue;
            }

            // Flash in/out every 6 ticks (visible for 6, invisible for 6, repeat)
            // Gets faster in the last 2 seconds (40 ticks remaining)
            int flashInterval = remaining <= 40 ? 3 : 6;
            boolean shouldBeInvisible = (elapsed / flashInterval) % 2 == 0;
            player.setInvisible(shouldBeInvisible);

            // Portal particles around the player every 5 ticks
            if (elapsed % 5 == 0) {
                ServerWorld world = player.getServerWorld();
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        8, 0.3, 0.5, 0.3, 0.3);
            }

            // Failed teleport sounds at escalating moments
            if (elapsed == 10) {
                player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.2f);
            }
            if (elapsed == 40) {
                player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.15f);
                player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.6f, 0.4f);
            }
            if (elapsed == 75) {
                player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.2f, 0.1f);
                player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 0.3f);
            }
        }
    }
}


