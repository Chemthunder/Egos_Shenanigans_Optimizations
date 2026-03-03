package net.vainnglory.egoistical.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.vainnglory.egoistical.item.ModItems;
import net.vainnglory.egoistical.util.InventoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void greedRuneGlowEffect(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        if (self instanceof PlayerEntity && client.player != null && client.world != null && self.equals(client.player)) {
            if (InventoryHelper.hasItem(client.player, ModItems.GREED_RUNE)) {
                cir.setReturnValue(true);
            }
        }
    }
}
