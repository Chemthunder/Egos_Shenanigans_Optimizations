package net.vainnglory.egoistical.util;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class HuskHealthManager {
    private static final int RESTORATION_DELAY_TICKS = 300;
    private static final UUID HUSK_HEALTH_MODIFIER_ID = UUID.fromString("a1b2c3d4-aaaa-4000-8000-000000000000");

    private static final Map<UUID, List<HealthReduction>> activeReductions = new HashMap<>();

    public static void addReduction(UUID playerUUID, long currentWorldTime, float amount) {
        activeReductions.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                .add(new HealthReduction(currentWorldTime + RESTORATION_DELAY_TICKS, amount));
    }

    public static void tick(long currentWorldTime, net.minecraft.server.MinecraftServer server) {
        Iterator<Map.Entry<UUID, List<HealthReduction>>> playerIter = activeReductions.entrySet().iterator();
        while (playerIter.hasNext()) {
            Map.Entry<UUID, List<HealthReduction>> entry = playerIter.next();
            UUID playerUUID = entry.getKey();
            List<HealthReduction> reductions = entry.getValue();

            boolean changed = reductions.removeIf(r -> currentWorldTime >= r.expirationTime);

            if (changed) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);
                if (player != null) {
                    applyModifier(player, reductions);
                }
            }

            if (reductions.isEmpty()) {
                playerIter.remove();
            }
        }
    }

    public static void applyToPlayer(ServerPlayerEntity player, long currentWorldTime) {
        addReduction(player.getUuid(), currentWorldTime, 2.0f);
        List<HealthReduction> reductions = activeReductions.get(player.getUuid());
        applyModifier(player, reductions);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyModifier(ServerPlayerEntity player, List<HealthReduction> reductions) {
        var attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null) return;

        var existing = attribute.getModifier(HUSK_HEALTH_MODIFIER_ID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }

        float totalReduction = 0;
        if (reductions != null) {
            for (HealthReduction r : reductions) {
                totalReduction += r.amount;
            }
        }

        if (totalReduction > 0) {
            attribute.addPersistentModifier(new EntityAttributeModifier(
                    HUSK_HEALTH_MODIFIER_ID,
                    "husk_health_cost",
                    -totalReduction,
                    EntityAttributeModifier.Operation.ADDITION
            ));
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static float getTotalReduction(UUID playerUUID) {
        List<HealthReduction> reductions = activeReductions.get(playerUUID);
        if (reductions == null) return 0;
        float total = 0;
        for (HealthReduction r : reductions) {
            total += r.amount;
        }
        return total;
    }

    public static void cleanup(UUID playerUUID) {
        activeReductions.remove(playerUUID);
    }

    public static void cleanupFull(ServerPlayerEntity player) {
        activeReductions.remove(player.getUuid());
        var attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            var existing = attribute.getModifier(HUSK_HEALTH_MODIFIER_ID);
            if (existing != null) {
                attribute.removeModifier(existing);
            }
        }
    }

    private record HealthReduction(long expirationTime, float amount) {}
}
