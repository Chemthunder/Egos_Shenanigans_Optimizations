package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.vainnglory.egoistical.util.ForEgoOnlyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class CraftingRestrictMixin {

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void blockRestrictedCrafting(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!ForEgoOnlyManager.isActive(serverPlayer.getServer())) return;
        if (ForEgoOnlyManager.isOwner(player.getUuid())) return;

        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (!(handler instanceof CraftingScreenHandler)) return;
        if (slotIndex != 0) return;

        ItemStack result = handler.getSlot(0).getStack();
        if (result.isEmpty()) return;

        if (ForEgoOnlyManager.isRestricted(result.getItem())) {
            player.sendMessage(
                    Text.literal("You cannot craft this item.").formatted(Formatting.RED),
                    true
            );
            ci.cancel();
        }
    }
}


