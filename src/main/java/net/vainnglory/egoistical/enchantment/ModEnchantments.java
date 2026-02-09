package net.vainnglory.egoistical.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.Egoistical;

public class ModEnchantments {

    public static final Enchantment METAMORPHOSIS = Registry.register(
            Registries.ENCHANTMENT,
            new Identifier(Egoistical.MOD_ID, "metamorphosis"),
            new MetamorphosisEnchantment()
    );

    public static final Enchantment BLOOD_OFFERING = Registry.register(
            Registries.ENCHANTMENT,
            new Identifier(Egoistical.MOD_ID, "blood_offering"),
            new BloodOfferingEnchantment()
    );

    public static void registerEnchantments() {
        Egoistical.LOGGER.info("Registering Enchantments for " + Egoistical.MOD_ID);
    }
}
