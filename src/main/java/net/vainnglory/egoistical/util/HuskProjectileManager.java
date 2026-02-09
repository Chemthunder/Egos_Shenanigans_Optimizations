package net.vainnglory.egoistical.util;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HuskProjectileManager {
    private static final List<HuskProjectile> activeProjectiles = new ArrayList<>();
    private static final double SPEED = 1.5;
    private static final double HOMING_STRENGTH = 0.15;
    private static final int MAX_LIFETIME = 60;
    private static final double HIT_RADIUS = 1.0;
    private static final float DAMAGE = 6.0f;
    private static final int POISON_DURATION = 100;
    private static final int POISON_AMPLIFIER = 1;

    public static void fireProjectile(ServerWorld world, ServerPlayerEntity shooter, Vec3d startPos, Vec3d direction, ServerPlayerEntity target) {
        activeProjectiles.add(new HuskProjectile(
                world,
                shooter.getUuid(),
                startPos,
                direction.normalize().multiply(SPEED),
                target != null ? target.getUuid() : null,
                0
        ));
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        Iterator<HuskProjectile> iter = activeProjectiles.iterator();
        while (iter.hasNext()) {
            HuskProjectile proj = iter.next();
            proj.age++;

            if (proj.age > MAX_LIFETIME) {
                iter.remove();
                continue;
            }

            ServerWorld world = server.getWorld(proj.world.getRegistryKey());
            if (world == null) {
                iter.remove();
                continue;
            }

            if (proj.targetUUID != null) {
                ServerPlayerEntity target = server.getPlayerManager().getPlayer(proj.targetUUID);
                if (target != null && target.isAlive() && target.getWorld().getRegistryKey().equals(world.getRegistryKey())) {
                    Vec3d toTarget = target.getPos().add(0, target.getHeight() / 2, 0).subtract(proj.position).normalize();
                    proj.velocity = proj.velocity.normalize()
                            .multiply(1.0 - HOMING_STRENGTH)
                            .add(toTarget.multiply(HOMING_STRENGTH))
                            .normalize()
                            .multiply(SPEED);
                }
            }

            proj.position = proj.position.add(proj.velocity);

            world.spawnParticles(ParticleTypes.ENTITY_EFFECT,
                    proj.position.x, proj.position.y, proj.position.z,
                    3, 0.1, 0.1, 0.1, 0.0);
            world.spawnParticles(ParticleTypes.CRIT,
                    proj.position.x, proj.position.y, proj.position.z,
                    1, 0, 0, 0, 0.0);

            ServerPlayerEntity shooter = server.getPlayerManager().getPlayer(proj.shooterUUID);
            Box hitBox = new Box(
                    proj.position.x - HIT_RADIUS, proj.position.y - HIT_RADIUS, proj.position.z - HIT_RADIUS,
                    proj.position.x + HIT_RADIUS, proj.position.y + HIT_RADIUS, proj.position.z + HIT_RADIUS
            );

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.getUuid().equals(proj.shooterUUID)) continue;
                if (!player.isAlive()) continue;

                if (player.getBoundingBox().intersects(hitBox)) {
                    DamageSource damageSource = shooter != null
                            ? player.getDamageSources().playerAttack(shooter)
                            : player.getDamageSources().magic();

                    player.damage(damageSource, DAMAGE);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, POISON_DURATION, POISON_AMPLIFIER, false, true, true));

                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_SPIDER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

                    player.sendMessage(Text.literal("Venomous!").formatted(Formatting.DARK_GREEN), true);
                    if (shooter != null) {
                        shooter.sendMessage(Text.literal("Hit " + player.getName().getString()).formatted(Formatting.GOLD), true);
                    }

                    iter.remove();
                    break;
                }
            }
        }
    }

    public static void cleanup() {
        activeProjectiles.clear();
    }

    private static class HuskProjectile {
        ServerWorld world;
        UUID shooterUUID;
        Vec3d position;
        Vec3d velocity;
        UUID targetUUID;
        int age;

        HuskProjectile(ServerWorld world, UUID shooterUUID, Vec3d position, Vec3d velocity, UUID targetUUID, int age) {
            this.world = world;
            this.shooterUUID = shooterUUID;
            this.position = position;
            this.velocity = velocity;
            this.targetUUID = targetUUID;
            this.age = age;
        }
    }
}
