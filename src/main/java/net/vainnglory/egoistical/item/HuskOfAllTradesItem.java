package net.vainnglory.egoistical.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.vainnglory.egoistical.util.HuskHealthManager;
import net.vainnglory.egoistical.util.HuskProjectileManager;
import net.vainnglory.egoistical.util.ModRarities;
import net.minecraft.enchantment.EnchantmentHelper;
import net.vainnglory.egoistical.enchantment.ModEnchantments;
import net.vainnglory.egoistical.util.MetamorphosisManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class HuskOfAllTradesItem extends SwordItem {
    private static final String CHARGES_KEY = "HuskCharges";
    private static final String MODE_KEY = "HuskMode";
    private static final String LAST_HIT_KEY = "HuskLastHit";

    public static final String MODE_NONE = "none";
    public static final String MODE_SOUL_SAND = "soul_sand";
    public static final String MODE_SPIDER_EYE = "spider_eye";
    public static final String MODE_ENDER_PEARL = "ender_pearl";
    public static final String MODE_GHAST_TEAR = "ghast_tear";

    private static final int MAX_CHARGES = 3;
    private static final double ABILITY_RANGE = 20.0;

    private final ModRarities rarity;

    private static final UUID BLOOD_OFFERING_DAMAGE_ID = UUID.fromString("d4e7f8a9-1234-4b56-9abc-def012345678");
    private static final UUID BLOOD_OFFERING_SPEED_ID = UUID.fromString("d4e7f8a9-5678-4b56-9abc-def012345679");

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;

        boolean isHolding = selected || player.getOffHandStack() == stack;
        boolean hasBloodOffering = EnchantmentHelper.getLevel(ModEnchantments.BLOOD_OFFERING, stack) > 0;

        var damageAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        var speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);

        if (isHolding && hasBloodOffering) {
            if (damageAttr != null && damageAttr.getModifier(BLOOD_OFFERING_DAMAGE_ID) == null) {
                damageAttr.addTemporaryModifier(new EntityAttributeModifier(
                        BLOOD_OFFERING_DAMAGE_ID, "blood_offering_damage", 2.0,
                        EntityAttributeModifier.Operation.ADDITION));
            }
            if (speedAttr != null && speedAttr.getModifier(BLOOD_OFFERING_SPEED_ID) == null) {
                speedAttr.addTemporaryModifier(new EntityAttributeModifier(
                        BLOOD_OFFERING_SPEED_ID, "blood_offering_speed", -0.1,
                        EntityAttributeModifier.Operation.ADDITION));
            }
        } else {
            if (damageAttr != null && damageAttr.getModifier(BLOOD_OFFERING_DAMAGE_ID) != null) {
                damageAttr.removeModifier(BLOOD_OFFERING_DAMAGE_ID);
            }
            if (speedAttr != null && speedAttr.getModifier(BLOOD_OFFERING_SPEED_ID) != null) {
                speedAttr.removeModifier(BLOOD_OFFERING_SPEED_ID);
            }
        }
    }

    public HuskOfAllTradesItem(Settings settings, ModRarities rarity) {
        super(ToolMaterials.IRON, 4, -2.4f, settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().setStyle(Style.EMPTY.withColor(rarity.color));
    }


    public static int getCharges(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(CHARGES_KEY)) return 0;
        return nbt.getInt(CHARGES_KEY);
    }

    public static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateNbt().putInt(CHARGES_KEY, Math.max(0, Math.min(getMaxCharges(stack), charges)));
    }

    public static String getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(MODE_KEY)) return MODE_NONE;
        return nbt.getString(MODE_KEY);
    }

    public static void setMode(ItemStack stack, String mode) {
        stack.getOrCreateNbt().putString(MODE_KEY, mode);
    }

    @Nullable
    public static UUID getLastHitPlayer(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(LAST_HIT_KEY)) return null;
        try {
            return nbt.getUuid(LAST_HIT_KEY);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setLastHitPlayer(ItemStack stack, UUID uuid) {
        stack.getOrCreateNbt().putUuid(LAST_HIT_KEY, uuid);
    }


    public static float getChargePredicate(ItemStack stack) {
        return getCharges(stack) / (float) getMaxCharges(stack);
    }

    public static int getMaxCharges(ItemStack stack) {
        if (EnchantmentHelper.getLevel(ModEnchantments.BLOOD_OFFERING, stack) > 0) {
            return 2;
        }
        return MAX_CHARGES;
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof PlayerEntity && attacker instanceof PlayerEntity) {
            if (!MetamorphosisManager.isActive(attacker.getUuid())) {
                setLastHitPlayer(stack, target.getUuid());
            }
        }
        return super.postHit(stack, target, attacker);
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(user.getStackInHand(hand));

        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(stack);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (EnchantmentHelper.getLevel(ModEnchantments.METAMORPHOSIS, stack) > 0) {
            if (user.getHealth() <= 4.0f && !MetamorphosisManager.isActive(serverPlayer.getUuid())) {
                long worldTime = serverPlayer.getServer().getOverworld().getTime();
                if (!MetamorphosisManager.isOnCooldown(serverPlayer.getUuid(), worldTime)) {
                    MetamorphosisManager.activate(serverPlayer, worldTime);
                    return TypedActionResult.success(stack);
                }
            }
        }

        if (MetamorphosisManager.isActive(serverPlayer.getUuid())) {
            user.sendMessage(Text.literal("Cannot use abilities during Metamorphosis").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }

        if (user.isSneaking()) {
            return handleCharging(serverPlayer, stack);
        }

        return handleAbility(serverPlayer, stack, (ServerWorld) world);
    }

    private TypedActionResult<ItemStack> handleCharging(ServerPlayerEntity player, ItemStack stack) {
        int charges = getCharges(stack);
        int maxCharges = getMaxCharges(stack);
        if (charges >= maxCharges) {
            player.sendMessage(Text.literal("Already fully charged").formatted(Formatting.GOLD), true);
            return TypedActionResult.fail(stack);
        }

        float currentMax = player.getMaxHealth();
        if (currentMax - 2.0f < 2.0f) {
            player.sendMessage(Text.literal("Not enough vitality").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        HuskHealthManager.applyToPlayer(player, player.getServer().getOverworld().getTime());

        setCharges(stack, charges + 1);

        player.playSound(SoundEvents.ENTITY_WITHER_HURT, 0.5f, 1.5f);
        player.sendMessage(Text.literal("Charged (" + (charges + 1) + "/" + maxCharges + ")")
                .formatted(Formatting.GOLD), true);

        player.getItemCooldownManager().set(this, 10);

        return TypedActionResult.success(stack);
    }

    private TypedActionResult<ItemStack> handleAbility(ServerPlayerEntity player, ItemStack stack, ServerWorld world) {
        int charges = getCharges(stack);
        String mode = getMode(stack);

        if (charges <= 0) {
            player.sendMessage(Text.literal("No charges").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }

        if (mode.equals(MODE_NONE)) {
            player.sendMessage(Text.literal("No mode set").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }

        switch (mode) {
            case MODE_SOUL_SAND -> {
                return useSoulSand(player, stack, world);
            }
            case MODE_SPIDER_EYE -> {
                return useSpiderEye(player, stack, world);
            }
            case MODE_ENDER_PEARL -> {
                return useEnderPearl(player, stack, world);
            }
            case MODE_GHAST_TEAR -> {
                return useGhastTear(player, stack, world);
            }
            default -> {
                return TypedActionResult.fail(stack);
            }
        }
    }


    private TypedActionResult<ItemStack> useSoulSand(ServerPlayerEntity player, ItemStack stack, ServerWorld world) {
        ServerPlayerEntity target = getLastHitTarget(player, stack);
        if (target == null) {
            player.sendMessage(Text.literal("No target in range").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        target.setVelocity(0, -2.0, 0);
        target.velocityModified = true;

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 9, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 128, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 60, 4, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 9, false, false, true));


        target.damage(player.getDamageSources().magic(), 4.0f);


        BlockPos targetPos = target.getBlockPos().down();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos breakPos = targetPos.add(dx, 0, dz);
                if (!world.getBlockState(breakPos).isAir() && world.getBlockState(breakPos).getHardness(world, breakPos) >= 0) {
                    world.breakBlock(breakPos, true);
                }
            }
        }

        for (int i = 0; i < 40; i++) {
            world.spawnParticles(ParticleTypes.SOUL,
                    target.getX() + (world.random.nextDouble() - 0.5) * 2,
                    target.getY() + world.random.nextDouble() * 2,
                    target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    1, 0, 0.05, 0, 0.02);
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 9, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 50, 128, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 50, 4, false, false, true));

        target.sendMessage(Text.literal("Slammed!").formatted(Formatting.RED), true);
        player.sendMessage(Text.literal("Slammed " + target.getName().getString()).formatted(Formatting.GOLD), true);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.5f, 0.5f);

        setCharges(stack, getCharges(stack) - 1);
        player.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(stack);
    }

    private TypedActionResult<ItemStack> useSpiderEye(ServerPlayerEntity player, ItemStack stack, ServerWorld world) {
        UUID targetUUID = getLastHitPlayer(stack);
        ServerPlayerEntity target = targetUUID != null ? player.getServer().getPlayerManager().getPlayer(targetUUID) : null;

        boolean hasTarget = target != null
                && target.isAlive()
                && target.getWorld().getRegistryKey().equals(player.getWorld().getRegistryKey())
                && target.squaredDistanceTo(player) <= ABILITY_RANGE * ABILITY_RANGE;

        Vec3d startPos = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);

        HuskProjectileManager.fireProjectile(
                world,
                player,
                startPos,
                direction,
                hasTarget ? target : null
        );

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.5f);

        player.sendMessage(Text.literal("Fang fired").formatted(Formatting.GOLD), true);

        setCharges(stack, getCharges(stack) - 1);
        player.getItemCooldownManager().set(this, 15);
        return TypedActionResult.success(stack);
    }

    private TypedActionResult<ItemStack> useEnderPearl(ServerPlayerEntity player, ItemStack stack, ServerWorld world) {
        ServerPlayerEntity target = getLookedAtPlayer(player, ABILITY_RANGE);

        if (target != null) {
            Vec3d targetLook = target.getRotationVec(1.0f);
            Vec3d behindTarget = target.getPos().add(targetLook.multiply(-1.5));


            for (int i = 0; i < 32; i++) {
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.1);
            }

            player.teleport(behindTarget.x, target.getY(), behindTarget.z);

            for (int i = 0; i < 32; i++) {
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.1);
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(Text.literal("Behind you.").formatted(Formatting.GOLD), true);

            setCharges(stack, getCharges(stack) - 1);
            player.getItemCooldownManager().set(this, 20);
        } else {
            int charges = getCharges(stack);

            Vec3d lookVec = player.getRotationVec(1.0f);
            Vec3d destination = player.getPos().add(lookVec.multiply(30.0));

            for (int i = 0; i < 32; i++) {
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.1);
            }

            player.teleport(destination.x, destination.y, destination.z);

            for (int i = 0; i < 32; i++) {
                world.spawnParticles(ParticleTypes.PORTAL,
                        player.getX() + (world.random.nextDouble() - 0.5) * 2,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 2,
                        1, 0, 0, 0, 0.1);
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.8f);

            player.sendMessage(Text.literal("Blink.").formatted(Formatting.GOLD), true);

            setCharges(stack, 0);
            player.getItemCooldownManager().set(this, 40);
        }

        return TypedActionResult.success(stack);
    }

    private TypedActionResult<ItemStack> useGhastTear(ServerPlayerEntity player, ItemStack stack, ServerWorld world) {
        ServerPlayerEntity target = getLookedAtPlayer(player, ABILITY_RANGE);

        if (target == null) {
            player.sendMessage(Text.literal("No target in sight").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 2, false, true, true));

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 1, false, true, true));

        for (int i = 0; i < 30; i++) {
            world.spawnParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR,
                    target.getX() + (world.random.nextDouble() - 0.5) * 2,
                    target.getY() + 2 + world.random.nextDouble(),
                    target.getZ() + (world.random.nextDouble() - 0.5) * 2,
                    1, 0, 0, 0, 0.05);
        }

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_GHAST_SCREAM, SoundCategory.PLAYERS, 0.8f, 1.5f);

        target.sendMessage(Text.literal("Your arms grow heavy...").formatted(Formatting.RED), true);
        player.sendMessage(Text.literal("Cursed " + target.getName().getString()).formatted(Formatting.GOLD), true);

        setCharges(stack, 0);
        player.getItemCooldownManager().set(this, 40);
        return TypedActionResult.success(stack);
    }


    @Nullable
    private ServerPlayerEntity getLastHitTarget(ServerPlayerEntity user, ItemStack stack) {
        UUID targetUUID = getLastHitPlayer(stack);
        if (targetUUID == null) return null;

        ServerPlayerEntity target = user.getServer().getPlayerManager().getPlayer(targetUUID);
        if (target == null || !target.isAlive()) return null;
        if (!target.getWorld().getRegistryKey().equals(user.getWorld().getRegistryKey())) return null;
        if (target.squaredDistanceTo(user) > ABILITY_RANGE * ABILITY_RANGE) return null;

        return target;
    }

    @Nullable
    private ServerPlayerEntity getLookedAtPlayer(ServerPlayerEntity user, double range) {
        Vec3d eyePos = user.getEyePos();
        Vec3d lookVec = user.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.multiply(range));

        ServerPlayerEntity closest = null;
        double closestDist = range;

        for (ServerPlayerEntity player : user.getServerWorld().getPlayers()) {
            if (player == user || !player.isAlive()) continue;

            var box = player.getBoundingBox().expand(0.5);
            var hit = box.raycast(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = player;
                }
            }
        }

        return closest;
    }


    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        String mode = getMode(stack);
        int charges = getCharges(stack);

        tooltip.add(Text.literal("Charges: " + charges + "/" + getMaxCharges(stack)).formatted(Formatting.GRAY));

        if (!mode.equals(MODE_NONE)) {
            tooltip.add(Text.literal("Mode: ").formatted(Formatting.GRAY)
                    .append(Text.literal(getModeDisplayName(mode)).formatted(getModeColor(mode))));
        } else {
            tooltip.add(Text.literal("No mode set").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
    }

    public static String getModeDisplayName(String mode) {
        return switch (mode) {
            case MODE_SOUL_SAND -> "Soul Sand";
            case MODE_SPIDER_EYE -> "Spider Eye";
            case MODE_ENDER_PEARL -> "Ender Pearl";
            case MODE_GHAST_TEAR -> "Ghast Tear";
            default -> "None";
        };
    }

    private Formatting getModeColor(String mode) {
        return switch (mode) {
            case MODE_SOUL_SAND -> Formatting.DARK_AQUA;
            case MODE_SPIDER_EYE -> Formatting.DARK_RED;
            case MODE_ENDER_PEARL -> Formatting.DARK_PURPLE;
            case MODE_GHAST_TEAR -> Formatting.GOLD;
            default -> Formatting.GRAY;
        };
    }
}
