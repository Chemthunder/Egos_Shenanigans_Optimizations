package net.vainnglory.egoistical.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.vainnglory.egoistical.item.ModItems;

import java.util.List;
import java.util.UUID;

public class ForEgoOnlyManager {

    public static final UUID OWNER_UUID = UUID.fromString("d1848a30-b4c9-4f64-817d-0d09377b125c");

    private static final List<Item> RESTRICTED_ITEMS = List.of(
            ModItems.TRACKER,
            ModItems.PORTABLE_STASIS,
            ModItems.MARKSMANS_PROOF,
            ModItems.EMP,
            ModItems.GREED_RUNE
    );

    public static boolean isOwner(UUID uuid) {
        return OWNER_UUID.equals(uuid);
    }

    public static boolean isRestricted(Item item) {
        return RESTRICTED_ITEMS.contains(item);
    }

    public static boolean isActive(MinecraftServer server) {
        return server != null && server.getGameRules().getBoolean(ModGameRules.FOR_EGO_ONLY);
    }

    public static void stripRestrictedItems(ServerPlayerEntity player) {
        if (isOwner(player.getUuid())) return;

        var inventory = player.getInventory();
        boolean stripped = false;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (isRestricted(stack.getItem())) {
                inventory.setStack(i, ItemStack.EMPTY);
                stripped = true;
            }
        }

        if (stripped) {
            player.sendMessage(
                    Text.literal("Some items were removed: ForEgoOnly is active.").formatted(Formatting.RED),
                    false
            );
        }
    }

    public static void stripAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            stripRestrictedItems(player);
        }
    }
}

