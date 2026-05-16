package ru.arixcompany.utils.player.inv;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.features.repos.OtherRepo;
import ru.arixcompany.utils.IMinecraft;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class InventoryUtility implements IMinecraft {
    private static int cachedSlot = -1;

    public static int getItemCount(Item item) {
        if (mc.player == null) return 0;

        int counter = 0;

        for (int i = 0; i <= 44; ++i) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() != item) continue;
            counter += itemStack.getCount();
        }

        return counter;
    }

    public static SearchInvResult getAxe() {
        if (mc.player == null) return SearchInvResult.notFound();
        int slot = -1;
        float f = 1.0F;

        for (int b1 = 9; b1 < 45; b1++) {
            int realSlot = b1 >= 36 ? b1 - 36 : b1;
            ItemStack itemStack = mc.player.getInventory().getItem(realSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!(itemStack.getItem() instanceof AxeItem)) continue;

            ItemEnchantments enchants = itemStack.get(DataComponents.ENCHANTMENTS);
            if (enchants == null) continue;

            float f1 = 0;

            Integer maxDamage = itemStack.get(DataComponents.MAX_DAMAGE);
            if (maxDamage != null) f1 += maxDamage;

            if (mc.level != null) {
                var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var holder = lookup.get(Enchantments.SHARPNESS);
                if (holder.isPresent()) {
                    f1 += enchants.getLevel(holder.get());
                }
            }

            if (f1 > f) {
                f = f1;
                slot = b1;
            }
        }

        if (slot == -1) return SearchInvResult.notFound();
        if (slot >= 36) slot = slot - 36;

        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    public static SearchInvResult getPickAxeHotbar() {
        if (mc.player == null) return SearchInvResult.notFound();

        int slot = -1;
        float f = 0.0F;

        for (int b1 = 0; b1 < 9; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!isPickaxe(itemStack)) continue;

            ItemEnchantments enchants = itemStack.get(DataComponents.ENCHANTMENTS);
            if (enchants == null) continue;

            float f1 = 0;

            if (mc.level != null) {
                var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var holder = lookup.get(Enchantments.EFFICIENCY);
                if (holder.isPresent()) {
                    f1 += enchants.getLevel(holder.get());
                }
            }

            if (f1 > f) {
                f = f1;
                slot = b1;
            }
        }

        if (slot == -1) return SearchInvResult.notFound();
        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    public static SearchInvResult getPickAxe() {
        if (mc.player == null) return SearchInvResult.notFound();

        int slot = -1;
        float f = 0.0F;

        for (int b1 = 9; b1 < 45; b1++) {
            int realSlot = b1 >= 36 ? b1 - 36 : b1;
            ItemStack itemStack = mc.player.getInventory().getItem(realSlot);
            if (itemStack == null || itemStack.isEmpty()) continue;
            if (!isPickaxe(itemStack)) continue;

            ItemEnchantments enchants = itemStack.get(DataComponents.ENCHANTMENTS);
            if (enchants == null) continue;

            float f1 = 0;

            if (mc.level != null) {
                var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var holder = lookup.get(Enchantments.EFFICIENCY);
                if (holder.isPresent()) {
                    f1 += enchants.getLevel(holder.get());
                }
            }

            if (f1 > f) {
                f = f1;
                slot = b1;
            }
        }

        if (slot == -1) return SearchInvResult.notFound();
        if (slot >= 36) slot = slot - 36;

        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    public static SearchInvResult getSkull() {
        if (mc.player == null) return SearchInvResult.notFound();
        int slot = -1;
        for (int b1 = 0; b1 < 9; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1);
            if (itemStack != null &&
                    (itemStack.getItem().equals(Items.SKELETON_SKULL)
                            || itemStack.getItem().equals(Items.WITHER_SKELETON_SKULL)
                            || itemStack.getItem().equals(Items.CREEPER_HEAD)
                            || itemStack.getItem().equals(Items.PLAYER_HEAD)
                            || itemStack.getItem().equals(Items.ZOMBIE_HEAD))) {
                slot = b1;
                break;
            }
        }
        if (slot == -1) return SearchInvResult.notFound();
        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    private static int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        if (mc.level == null || stack == null || stack.isEmpty()) return 0;
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return 0;
        var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var holder = lookup.get(key);
        if (holder.isEmpty()) return 0;
        return enchants.getLevel(holder.get());
    }

    public static SearchInvResult getSword() {
        if (mc.player == null) return SearchInvResult.notFound();

        int slot = -1;
        float f = -1.0F;
        for (int b1 = 9; b1 < 45; b1++) {
            int realSlot = b1 >= 36 ? b1 - 36 : b1;
            ItemStack itemStack = mc.player.getInventory().getItem(realSlot);

            if (!itemStack.isEmpty() && isSword(itemStack)) {
                float f1 = 0;

                Integer maxDamage = itemStack.get(DataComponents.MAX_DAMAGE);
                if (maxDamage != null) f1 += maxDamage;

                f1 += getEnchantLevel(itemStack, Enchantments.SHARPNESS);

                if (f1 > f) {
                    f = f1;
                    slot = b1;
                }
            }
        }

        if (slot == -1) return SearchInvResult.notFound();

        if (slot >= 36) slot = slot - 36;

        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    public static SearchInvResult getSwordHotBar() {
        if (mc.player == null) return SearchInvResult.notFound();

        int slot = -1;
        float f = -1.0F;
        for (int b1 = 0; b1 < 9; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1);

            if (!itemStack.isEmpty() && isSword(itemStack)) {
                float f1 = 0;

                Integer maxDamage = itemStack.get(DataComponents.MAX_DAMAGE);
                if (maxDamage != null) f1 += maxDamage;

                f1 += getEnchantLevel(itemStack, Enchantments.SHARPNESS);

                if (f1 > f) {
                    f = f1;
                    slot = b1;
                }
            }
        }

        if (slot == -1) return SearchInvResult.notFound();
        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }

    public static SearchInvResult getAxeHotBar() {
        if (mc.player == null) return SearchInvResult.notFound();

        int slot = -1;
        float f = -1.0F;
        for (int b1 = 0; b1 < 9; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1);

            if (!itemStack.isEmpty() && isAxe(itemStack)) {
                float f1 = 0;

                Integer maxDamage = itemStack.get(DataComponents.MAX_DAMAGE);
                if (maxDamage != null) f1 += maxDamage;

                f1 += getEnchantLevel(itemStack, Enchantments.SHARPNESS);

                if (f1 > f) {
                    f = f1;
                    slot = b1;
                }
            }
        }

        if (slot == -1) return SearchInvResult.notFound();
        return new SearchInvResult(slot, true, mc.player.getInventory().getItem(slot));
    }


    public static int getElytra() {
        if (mc.player == null) return -1;

        ItemStack chestStack = mc.player.getInventory().getItem(38);

        if (!chestStack.isEmpty() && chestStack.getItem() == Items.ELYTRA) {
            if ((chestStack.getMaxDamage() - chestStack.getDamageValue()) > 1) {
                return -2;
            }
        }

        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);

            if (!s.isEmpty() && s.getItem() == Items.ELYTRA) {
                int remainingDurability = s.getMaxDamage() - s.getDamageValue();

                if (remainingDurability > 1) {
                    slot = i;
                    break;
                }
            }
        }

        if (slot == -1) return -1;

        if (slot < 9) {
            slot = slot + 36;
        }

        return slot;
    }

    public static SearchInvResult findInHotBar(Searcher searcher) {
        if (mc.player != null) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (searcher.isValid(stack)) {
                    return new SearchInvResult(i, true, stack);
                }
            }
        }

        return SearchInvResult.notFound();
    }

    public static SearchInvResult findItemInHotBar(List<Item> items) {
        return findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static SearchInvResult findItemInHotBar(Item... items) {
        return findItemInHotBar(Arrays.asList(items));
    }

    public static SearchInvResult findInInventory(Searcher searcher) {
        if (mc.player != null) {
            for (int i = 36; i >= 0; i--) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (searcher.isValid(stack)) {
                    if (i < 9) i += 36;
                    return new SearchInvResult(i, true, stack);
                }
            }
        }

        return SearchInvResult.notFound();
    }

    public static SearchInvResult findItemInInventory(List<Item> items) {
        return findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static SearchInvResult findItemInInventory(Item... items) {
        return findItemInInventory(Arrays.asList(items));
    }

    public static SearchInvResult findBlockInHotBar(@NotNull List<Block> blocks) {
        return findItemInHotBar(blocks.stream().map(Block::asItem).toList());
    }

    public static SearchInvResult findBlockInHotBar(Block... blocks) {
        return findItemInHotBar(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static SearchInvResult findBlockInInventory(@NotNull List<Block> blocks) {
        return findItemInInventory(blocks.stream().map(Block::asItem).toList());
    }

    public static SearchInvResult findBlockInInventory(Block... blocks) {
        return findItemInInventory(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static void saveSlot() {
        cachedSlot = mc.player.getInventory().getSelectedSlot();
    }

    public static void returnSlot() {
        if (cachedSlot != -1)
            switchTo(cachedSlot);
        cachedSlot = -1;
    }

    public static void saveAndSwitchTo(int slot) {
        saveSlot();
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getInventory().getSelectedSlot() == slot && OtherRepo.serverSideSlot == slot)
            return;
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.ensureHasSentCarriedItem();
    }

    public static void switchTo(int slot) {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getInventory().getSelectedSlot() == slot && OtherRepo.serverSideSlot == slot) return;

        mc.player.getInventory().setSelectedSlot(slot);

        mc.gameMode.ensureHasSentCarriedItem();
    }

    public static void switchToSilent(int slot) {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }

    public static SearchInvResult getAntiWeaknessItem() {
        if (mc.player == null) return SearchInvResult.notFound();

        ItemStack mainHandStack = mc.player.getMainHandItem();
        if (isAntiWeakness(mainHandStack)) {
            return new SearchInvResult(mc.player.getInventory().getSelectedSlot(), true, mainHandStack);
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && isAntiWeakness(stack)) {
                return new SearchInvResult(i, true, stack);
            }
        }

        return SearchInvResult.notFound();
    }

    private static boolean isAntiWeakness(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = getItemId(stack);

        return id.contains("sword")
                || id.contains("axe")
                || id.contains("pickaxe")
                || id.contains("shovel")
                || id.contains("hoe");
    }

    public static float getHitDamage(@NotNull ItemStack weapon, Player ent) {
        if (mc.player == null) return 0;
        float baseDamage = 1f;

        if (isSword(weapon))
            baseDamage = 7;

        if (weapon.getItem() instanceof AxeItem axeItem)
            baseDamage = 9;

        if (mc.player.fallDistance > 0)
            baseDamage += baseDamage / 2f;

        if (mc.player.hasEffect(MobEffects.STRENGTH)) {
            int strength = Objects.requireNonNull(mc.player.getEffect(MobEffects.STRENGTH)).getAmplifier() + 1;
            baseDamage += 3 * strength;
        }

        baseDamage = CombatRules.getDamageAfterAbsorb(ent, baseDamage, mc.level.damageSources().generic(), ent.getArmorValue(), (float) ent.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue());
        return baseDamage;
    }

    public static SearchInvResult findBedInHotBar() {
        if (mc.player == null) return SearchInvResult.notFound();
        for (int b1 = 0; b1 < 9; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1);
            if (itemStack != null && itemStack.getItem() instanceof BedItem)
                return new SearchInvResult(b1, true, mc.player.getInventory().getItem(b1));
        }
        return SearchInvResult.notFound();
    }

    public static SearchInvResult findBed() {
        if (mc.player == null) return SearchInvResult.notFound();
        for (int b1 = 9; b1 < 45; b1++) {
            ItemStack itemStack = mc.player.getInventory().getItem(b1 >= 36 ? b1 - 36 : b1);
            if (itemStack != null && itemStack.getItem() instanceof BedItem)
                return new SearchInvResult(b1, true, mc.player.getInventory().getItem(b1));
        }
        return SearchInvResult.notFound();
    }

    public static Item getItem(String name) {
        if (name == null || name.isEmpty()) return Items.AIR;
        String cleanName = name.toLowerCase()
                .replace("item.minecraft.", "")
                .replace("block.minecraft.", "");

        try {
            Identifier id = Identifier.parse(cleanName);
            Item item = BuiltInRegistries.ITEM.getValue(id);

            if (item != null && item != Items.AIR) {
                return item;
            }

            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != Blocks.AIR) {
                return block.asItem();
            }

        } catch (Exception e) {
            return Items.DIRT;
        }

        return Items.DIRT;
    }

    public static int getBedsCount() {
        if (mc.player == null) return 0;

        int counter = 0;

        for (int i = 0; i <= 44; ++i) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (!(itemStack.getItem() instanceof BedItem)) continue;
            counter += itemStack.getCount();
        }

        return counter;
    }

    public static void clickSlot(int id) {
        if (id == -1 || mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, id, 0, ClickType.PICKUP, mc.player);
    }

    public static void clickSlot(Slot slot, int button, ClickType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.index, button, clickType, silent);
    }

    public static void clickSlot(int slotId, int buttonId, ClickType clickType, boolean silent) {
        clickSlot(mc.player.containerMenu.containerId, slotId, buttonId, clickType, silent);
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, ClickType clickType, boolean silent) {
        mc.gameMode.handleInventoryMouseClick(windowId, slotId, buttonId, clickType, mc.player);
        if (silent) mc.player.containerMenu.clicked(slotId, buttonId, clickType, mc.player);
    }

    private static boolean isAxe(ItemStack stack) {
        String id = getItemId(stack);
        return id.contains("axe") && !id.contains("pickaxe");
    }

    private static boolean isSword(ItemStack stack) {
        String id = getItemId(stack);
        return id.contains("sword");
    }

    private static boolean isPickaxe(ItemStack stack) {
        String id = getItemId(stack);
        return id.contains("pickaxe");
    }

    private static String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    public interface Searcher {
        boolean isValid(ItemStack stack);
    }
}