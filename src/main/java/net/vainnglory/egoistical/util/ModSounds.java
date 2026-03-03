package net.vainnglory.egoistical.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.vainnglory.egoistical.Egoistical;

public class ModSounds {

    public static final SoundEvent PALE = registerSoundEvent("the_pale_riseth");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(Egoistical.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        Egoistical.LOGGER.info("Reggistering Sounds for Bounty Hunting with " + Egoistical.MOD_ID);
    }
}
