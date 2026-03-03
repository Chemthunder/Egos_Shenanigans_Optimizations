package net.vainnglory.egoistical.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.vainnglory.egoistical.util.EMPManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    @Unique
    private static final String STORED_ENCHANTS_KEY = "EgoisticalStoredEnchants";

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void restoreEnchantsOnPickup(PlayerEntity player, CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        ItemStack stack = itemEntity.getStack();

        if (!EMPManager.isAffected(player.getUuid) && !stack.isEmpty) {
            NbtCompound nbt = stack.getNbt();

            if (nbt != null && nbt.contains(STORED_ENCHANTS_KEY)) {
                NbtList storedEnchants = nbt.getList(STORED_ENCHANTS_KEY, 10);
                nbt.put("Enchantments", storedEnchants);
                nbt.remove(STORED_ENCHANTS_KEY);
            }
        }
    }
}
