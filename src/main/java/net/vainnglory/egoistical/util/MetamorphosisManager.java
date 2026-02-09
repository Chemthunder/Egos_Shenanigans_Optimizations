package net.vainnglory.egoistical.util;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class MetamorphosisManager {
    private static final int DURATION_TICKS = 300;
    private static final int COOLDOWN_TICKS = 3600;

    private static final Map<UUID, Long> activeMetamorphosis = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static boolean isActive(UUID playerUUID) {
        return activeMetamorphosis.containsKey(playerUUID);
    }

    public static boolean isOnCooldown(UUID playerUUID, long currentWorldTime) {
        Long cooldownEnd = cooldowns.get(playerUUID);
        if (cooldownEnd == null) return false;
        if (currentWorldTime >= cooldownEnd) {
            cooldowns.remove(playerUUID);
            return false;
        }
        return true;
    }

    public static void activate(ServerPlayerEntity player, long currentWorldTime) {
        UUID uuid = player.getUuid();
        activeMetamorphosis.put(uuid, currentWorldTime + DURATION_TICKS);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, DURATION_TICKS, 0, false, false, true));
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.sendAbilitiesUpdate();

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BAT_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.8f);

        player.sendMessage(Text.literal("Metamorphosis!").formatted(Formatting.DARK_PURPLE), true);
    }

    public static void tick(long currentWorldTime, net.minecraft.server.MinecraftServer server) {
        Iterator<Map.Entry<UUID, Long>> iter = activeMetamorphosis.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Long> entry = iter.next();
            UUID uuid = entry.getKey();
            long expiration = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) {
                iter.remove();
                continue;
            }

            if (currentWorldTime >= expiration) {
                deactivate(player, currentWorldTime);
                iter.remove();
                continue;
            }

            if (player.getWorld() instanceof ServerWorld serverWorld) {
                if (currentWorldTime % 5 == 0) {
                    for (int i = 0; i < 5; i++) {
                        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                                player.getX() + (serverWorld.random.nextDouble() - 0.5) * 3,
                                player.getY() + 1 + serverWorld.random.nextDouble() * 2,
                                player.getZ() + (serverWorld.random.nextDouble() - 0.5) * 3,
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }

                if (currentWorldTime % 40 == 0) {
                    serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BAT_LOOP, SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
        }
    }

    private static void deactivate(ServerPlayerEntity player, long currentWorldTime) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }

        player.removeStatusEffect(StatusEffects.INVISIBILITY);

        cooldowns.put(player.getUuid(), currentWorldTime + COOLDOWN_TICKS);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BAT_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);

        player.sendMessage(Text.literal("Metamorphosis ended").formatted(Formatting.GRAY), true);
    }

    public static void cleanup(UUID playerUUID) {
        activeMetamorphosis.remove(playerUUID);
        cooldowns.remove(playerUUID);
    }
}
