package net.vainnglory.egoistical.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.vainnglory.egoistical.util.EMPManager;
import net.vainnglory.egoistical.util.ModRarities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EMPItem extends Item {
    private final ModRarities rarity;
    private static final int COOLDOWN_TICKS = 2400; // 2 minutes
    private static final int EMP_DURATION_TICKS = 700;
    private static final double EMP_RANGE = 50.0;

    public EMPItem(Settings settings, ModRarities rarity) {
        super(settings);
        this.rarity = rarity;
    }

    @Override
    public Text getName(ItemStack stack) {
        return super.getName(stack).copy().styled(style -> style.withColor(rarity.color));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!user.getItemCooldownManager.isCoolingDown(this)) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                ServerWorld serverWorld = (ServerWorld) world;

                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 2.0f, 0.5f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.5f);

                for (ServerPlayerEntity target : serverWorld.getPlayers()) {
                    if (target.squaredDistanceTo(user) <= EMP_RANGE * EMP_RANGE) {
                        EMPManager.disableEnchantments(target, EMP_DURATION_TICKS);

                        for (int i = 0; i < target.getInventory().size(); i++) {
                            ItemStack invStack = target.getInventory().getStack(i);
                            if (invStack.getItem() instanceof EMPItem empItem) {
                                target.getItemCooldownManager().set(empItem, COOLDOWN_TICKS);
                            }
                        }

                        if (target != user) {
                            target.sendMessage(Text.literal("Your equipment has been disrupted!?").formatted(Formatting.RED), true);
                        }
                    }
                }

                serverPlayer.sendMessage(Text.literal("EMP activated").formatted(Formatting.GOLD), true);
                user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }
    }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Disables all enchantments nearby").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Duration: 35 seconds").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Cooldown: 2 minutes").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("Disables nearby EMPs on use").formatted(Formatting.DARK_GRAY));
    }
}
