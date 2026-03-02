package net.vainnglory.egoistical.util;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class ModGameRules {

    public static final GameRules.Key<GameRules.BooleanRule> FOR_EGO_ONLY =
            GameRuleRegistry.register(
                    "forEgoOnly",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false, (server, rule) -> {
                        if (rule.get()) {
                            ForEgoOnlyManager.stripAllPlayers(server);
                        }
                    })
            );

    public static void registerGameRules() {
    }
}


