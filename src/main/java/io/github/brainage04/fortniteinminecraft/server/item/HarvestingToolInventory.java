package io.github.brainage04.fortniteinminecraft.server.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class HarvestingToolInventory {
    private static boolean registered;

    private HarvestingToolInventory() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ServerPlayerEvents.JOIN.register(HarvestingToolInventory::enforce);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> enforce(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(HarvestingToolInventory::enforce));
        registered = true;
    }

    public static boolean enforce(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        Inventory inventory = player.getInventory();
        ItemStack carried = player.containerMenu.getCarried();
        boolean hasCarriedTool = isHarvestingTool(carried);
        int keptSlot = -1;
        boolean changed = false;

        if (hasCarriedTool && carried.getCount() != 1) {
            carried.setCount(1);
            changed = true;
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isHarvestingTool(stack)) {
                continue;
            }
            if (!hasCarriedTool && keptSlot == -1) {
                keptSlot = slot;
                if (stack.getCount() != 1) {
                    stack.setCount(1);
                    changed = true;
                }
            } else {
                inventory.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (!hasCarriedTool && keptSlot == -1) {
            int restoreSlot = restoreSlot(inventory);
            ItemStack displaced = inventory.getItem(restoreSlot);
            if (!displaced.isEmpty()) {
                player.drop(displaced.copy(), false);
            }
            inventory.setItem(restoreSlot, new ItemStack(ModItems.PICKAXE));
            changed = true;
        }

        if (changed) {
            sync(player);
        }
        return changed;
    }

    public static boolean isHarvestingTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PickaxeItem;
    }

    public static void sync(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastFullState();
        player.containerMenu.broadcastFullState();
    }

    private static int restoreSlot(Inventory inventory) {
        int freeSlot = inventory.getFreeSlot();
        if (freeSlot != Inventory.NOT_FOUND_INDEX) {
            return freeSlot;
        }
        return inventory.getSelectedSlot();
    }
}
