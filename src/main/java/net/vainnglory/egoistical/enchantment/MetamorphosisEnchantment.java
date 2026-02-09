package net.vainnglory.egoistical.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.vainnglory.egoistical.item.HuskOfAllTradesItem;

public class MetamorphosisEnchantment extends Enchantment {

    public MetamorphosisEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof HuskOfAllTradesItem;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return !(other instanceof BloodOfferingEnchantment) && super.canAccept(other);
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return true;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true;
    }
}
