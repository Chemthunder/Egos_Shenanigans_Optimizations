package net.vainnglory.egoistical.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.vainnglory.egoistical.enchantment.ModEnchantments;
import net.vainnglory.egoistical.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class TrueInvisibilityMixin {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void hidePlayerDuringMetamorphosis(LivingEntity entity, float yaw, float tickDelta,
                                               MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                               int light, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                ItemStack mainHand = player.getMainHandStack();
                if (mainHand.isOf(ModItems.HUSK_OF_ALL_TRADES)) {
                    ci.cancel();
                }
            }
        }
    }
}