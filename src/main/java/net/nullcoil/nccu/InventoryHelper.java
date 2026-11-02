package net.nullcoil.nccu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Objects;

public class InventoryHelper {

    private static ItemStack rememberedChestplate = ItemStack.EMPTY;
    public static int stopGlidingCooldown = 0;
    public static int glidingStartCooldown = 0;
    public static int lastStartedGlidingTick = Integer.MIN_VALUE;
    public static boolean reEquipPending = false;
    public static int reEquipDelay = 0;

    /**
     * Swaps the player's current chestplate with the best elytra in their inventory.
     * Remembers the chestplate for swapping back later.
     * Called when trying to start gliding.
     */
    public static void swapChestplateWithBestElytra(Minecraft client) {
        Player player = client.player;
        if (player == null) return;

        ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (currentChest.has(DataComponents.GLIDER)) return;

        int slot = findBestElytraSlot(player);
        if (slot == -1) return;

        rememberedChestplate = currentChest.isEmpty() ? ItemStack.EMPTY : currentChest.copy();

        performSwap(client, slot);
        InventoryHelper.stopGlidingCooldown = 1;
    }

    /**
     * Swaps the player's current elytra back to the remembered chestplate if it still exists
     * in the inventory. If not found, just unequips the elytra to inventory.
     * Called when landing or canceling glide.
     */
    public static boolean swapElytraToBestChestplate(Minecraft client) {
        Player player = client.player;
        if (player == null) return false;

        ItemStack currentChest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!currentChest.has(DataComponents.GLIDER)) {
            return false;
        }

        int slot = findRememberedChestplateSlot(player);

        if (slot == -1) {
            return false;
        }

        performSwap(client, slot);
        rememberedChestplate = ItemStack.EMPTY;

        return true;
    }

    /**
     * Looks for the remembered chestplate in the player's inventory.
     * Matches by item type and enchantments, ignoring durability.
     * Returns its slot, or -1 if not found or nothing was remembered.
     */
    private static int findRememberedChestplateSlot(Player player) {
        if (rememberedChestplate.isEmpty()) return -1;

        Inventory inventory = player.getInventory();

        for (int i = 0; i < 36; i++) {
            if (isSameChestplate(inventory.getItem(i), rememberedChestplate)) {
                return i;
            }
        }

        if (isSameChestplate(inventory.getItem(Inventory.SLOT_OFFHAND), rememberedChestplate)) {
            return Inventory.SLOT_OFFHAND;
        }

        return -1;
    }

    /**
     * Matches by item type and enchantments, ignoring durability.
     * Handles inventories with Mending-based repair mods correctly.
     */
    private static boolean isSameChestplate(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItem(a, b)) return false;
        return Objects.equals(a.get(DataComponents.ENCHANTMENTS), b.get(DataComponents.ENCHANTMENTS));
    }

    /**
     * Finds the inventory slot of the elytra with the highest durability.
     * Never picks one with Curse of Binding.
     */
    private static int findBestElytraSlot(Player player) {
        Inventory inventory = player.getInventory();
        int bestSlot = -1;
        int bestDurability = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.has(DataComponents.GLIDER)) continue;
            if (hasCurseOfBinding(player, stack)) continue;

            int durability = stack.getMaxDamage() - stack.getDamageValue();
            if (durability > bestDurability) {
                bestDurability = durability;
                bestSlot = i;
            }
        }

        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (!offhand.isEmpty() && offhand.has(DataComponents.GLIDER) && !hasCurseOfBinding(player, offhand)) {
            int durability = offhand.getMaxDamage() - offhand.getDamageValue();
            if (durability > bestDurability) {
                bestSlot = Inventory.SLOT_OFFHAND;
            }
        }

        return bestSlot;
    }

    private static boolean hasCurseOfBinding(Player player, ItemStack stack) {
        return getEnchantLevel(player, stack) > 0;
    }

    private static int getEnchantLevel(Player player, ItemStack stack) {
        try {
            Holder<Enchantment> holder = player.level()
                    .registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.BINDING_CURSE);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Performs a three-click swap: pick up from inventory slot, place into chest slot, put remainder back.
     * If inventorySlot is -1, only unequips the chest slot to inventory.
     */
    private static void performSwap(Minecraft client, int inventorySlot) {
        Player player = client.player;
        if (player == null) return;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return;

        int containerId = player.inventoryMenu.containerId;
        int chestScreenSlot = 6;

        if (inventorySlot == -1) {
            gameMode.handleContainerInput(containerId, chestScreenSlot, 0, ContainerInput.PICKUP, player);
            gameMode.handleContainerInput(containerId, chestScreenSlot, 0, ContainerInput.PICKUP, player);
            return;
        }

        int screenSlot = inventoryToScreenSlot(inventorySlot);
        if (screenSlot == -1) return;

        gameMode.handleContainerInput(containerId, screenSlot, 0, ContainerInput.PICKUP, player);
        gameMode.handleContainerInput(containerId, chestScreenSlot, 0, ContainerInput.PICKUP, player);
        gameMode.handleContainerInput(containerId, screenSlot, 0, ContainerInput.PICKUP, player);
    }

    private static int inventoryToScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot;
        } else if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot;
        } else if (inventorySlot == Inventory.SLOT_OFFHAND) {
            return 45;
        }
        return -1;
    }

    public static void stopFallFlying(Minecraft client) {
        if (client.player == null) return;
        client.player.connection.send(new ServerboundPlayerCommandPacket(
                client.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    /**
     * Instant glide cancel: unequip the elytra and immediately re-equip it.
     * The server will stop the glide when the elytra is briefly removed.
     * Returns false if the elytra is cursed with Binding (can't be removed).
     */
    public static boolean cancelGlideByReequip(Minecraft client) {
        Player player = client.player;
        if (player == null) return false;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);

        if (!chestStack.has(DataComponents.GLIDER)) {
            return false;
        }

        if (hasCurseOfBinding(player, chestStack)) {
            return false;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return false;

        int containerId = player.inventoryMenu.containerId;
        int chestScreenSlot = 6;

        // Pick up the Elytra and leave it on the cursor.
        gameMode.handleContainerInput(
                containerId,
                chestScreenSlot,
                0,
                ContainerInput.PICKUP,
                player
        );

        reEquipPending = true;
        reEquipDelay = 1;

        return true;
    }

    public static void tickPendingReequip(Minecraft client) {
        if (!reEquipPending) {
            return;
        }

        if (reEquipDelay > 0) {
            reEquipDelay--;
            return;
        }

        Player player = client.player;
        if (player == null) {
            reEquipPending = false;
            return;
        }

        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) {
            reEquipPending = false;
            return;
        }

        int containerId = player.inventoryMenu.containerId;
        int chestScreenSlot = 6;

        gameMode.handleContainerInput(
                containerId,
                chestScreenSlot,
                0,
                ContainerInput.PICKUP,
                player
        );

        reEquipPending = false;
    }
}