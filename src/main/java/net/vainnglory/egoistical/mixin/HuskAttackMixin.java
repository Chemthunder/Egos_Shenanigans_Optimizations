package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.vainnglory.egoistical.util.MetamorphosisManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class HuskAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventAttackDuringMetamorphosis(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (MetamorphosisManager.isActive(serverPlayer.getUuid())) {
                serverPlayer.sendMessage(Text.literal("You can't attack during Metamorphosis")
                        .formatted(Formatting.GRAY), true);
                ci.cancel();
            }
        }
    }
}
