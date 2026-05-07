package ru.arixcompany.features.module.modules.misc.funtime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.render.EventHandledScreen;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AuctionUtils extends Module {

    private final BooleanSetting checkEnchants = new BooleanSetting("Проверка чар");
    private final BooleanSetting checkPotions = new BooleanSetting("Проверка зелий");
    private final BooleanSetting nelomanaya = new BooleanSetting("Больше 65% прочности");
    private final BooleanSetting removeThorns = new BooleanSetting("Убрать шипы");
    private final BooleanSetting removeKnockback = new BooleanSetting("Убрать отдачу");

    private final ListSetting armorEnchants = new ListSetting(
            "Чары брони"
    ).value("Защита 5","Починка")
            .visible(checkEnchants::isValue);

    private final SelectSetting sharpnessLevel = new SelectSetting(
            "Острота меча"
    ).value("Любая", "Острота 5", "Острота 6", "Острота 7")
            .visible(checkEnchants::isValue);

    private final ListSetting swordEnchants = new ListSetting(
            "Чары меча"
    ).value("Острота","Заговор огня")
            .visible(checkEnchants::isValue);

    private final ListSetting potionEffects = new ListSetting(
            "Эффекты зелий"
    ).value("Сила III", "Скорость III", "Исцеление II")
            .visible(checkPotions::isValue);

    public AuctionUtils() {
        super("AuctionUtils", Category.Misc);
        setup(
                checkEnchants,
                checkPotions,
                nelomanaya,
                removeKnockback,
                removeThorns,
                armorEnchants,
                sharpnessLevel,
                swordEnchants,
                potionEffects
        );
    }

    @EventHandler
    public void onHandledScreen(EventHandledScreen event) {
        if (!(mc.screen instanceof ContainerScreen screen)) return;
        if (mc.level == null) return;

        int offsetX = (screen.width - event.getBackgroundWidth()) / 2;
        int offsetY = (screen.height - event.getBackgroundHeight()) / 2;

        List<Slot> slots = screen.getMenu().slots;

        Slot cheapestTotal = findCheapest(slots, false);
        Slot cheapestPerItem = findCheapest(slots, true);

        GuiGraphics context = event.getGuiGraphics();
        context.pose().pushMatrix();
        context.pose().translate(offsetX, offsetY);

        if (cheapestTotal != null) {
            highlightSlot(context, cheapestTotal, getBlinkingColor(new Color(0, 220, 0).getRGB()));
            renderPriceLabel(context, cheapestTotal,
                    FuntimeUtil.getPrice(cheapestTotal.getItem()),
                    0xFF55FF55);
        }

        if (cheapestPerItem != null && cheapestPerItem != cheapestTotal) {
            highlightSlot(context, cheapestPerItem, getBlinkingColor(new Color(200, 100, 255).getRGB()));
        }

        if (checkEnchants.isValue()) {
            List<Slot> goodSlots = new ArrayList<>();

            for (Slot slot : slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;
                if (!hasValidPrice(slot)) continue;
                if (!passesDurabilityCheck(stack)) continue;

                boolean good = false;
                if (isArmor(stack) && isArmorGood(stack)) good = true;
                else if (isSword(stack) && isSwordGood(stack)) good = true;

                if (good) goodSlots.add(slot);
            }

            goodSlots.sort(Comparator.comparingInt(
                    s -> FuntimeUtil.getPrice(s.getItem())));

            int topColor = getBlinkingColor(new Color(255, 215, 0).getRGB());
            int goodColor = getBlinkingColor(new Color(100, 180, 255).getRGB());

            for (int i = 0; i < goodSlots.size(); i++) {
                Slot slot = goodSlots.get(i);

                if (slot == cheapestTotal || slot == cheapestPerItem) continue;

                if (i < 3) {
                    highlightSlot(context, slot, topColor);
                    renderRank(context, slot, i + 1);
                    renderPriceLabel(context, slot,
                            FuntimeUtil.getPrice(slot.getItem()),
                            0xFFFFD700);
                } else {
                    highlightSlot(context, slot, goodColor);
                }
            }
        }

        if (checkPotions.isValue()) {
            List<Slot> potionSlots = new ArrayList<>();

            for (Slot slot : slots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;
                if (!hasValidPrice(slot)) continue;

                PotionMark mark = getPotionMark(stack);
                if (mark == PotionMark.NONE) continue;

                potionSlots.add(slot);
            }

            potionSlots.sort(Comparator.comparingInt(
                    s -> FuntimeUtil.getPrice(s.getItem())));

            int maxShow = Math.min(potionSlots.size(), 3);

            for (int i = 0; i < maxShow; i++) {
                Slot slot = potionSlots.get(i);

                if (slot == cheapestTotal || slot == cheapestPerItem) continue;

                PotionMark mark = getPotionMark(slot.getItem());

                int blinkColor = switch (mark) {
                    case STRENGTH_III -> getBlinkingColor(new Color(255, 70, 70).getRGB());
                    case SPEED_III -> getBlinkingColor(new Color(85, 170, 255).getRGB());
                    case HEAL_II -> getBlinkingColor(new Color(255, 130, 170).getRGB());
                    case BOTH_III -> getBlinkingColor(new Color(170, 70, 255).getRGB());
                    default -> 0;
                };

                int textColor = switch (mark) {
                    case STRENGTH_III -> 0xFFFF5555;
                    case SPEED_III -> 0xFF55AAFF;
                    case HEAL_II -> 0xFFFF82AA;
                    case BOTH_III -> 0xFFAA55FF;
                    default -> 0xFFFFFFFF;
                };

                highlightSlot(context, slot, blinkColor);
                renderRank(context, slot, i + 1);
                renderPriceLabel(context, slot,
                        FuntimeUtil.getPrice(slot.getItem()),
                        textColor);
            }
        }

        context.pose().popMatrix();

        for (Slot slot : slots) {
            if (FuntimeUtil.hasPrice(slot.getItem())) {
                FuntimeUtil.addPriceToTooltip(slot.getItem());
            }
        }
    }

    private boolean passesDurabilityCheck(ItemStack stack) {
        if (!nelomanaya.isValue()) return true;
        if (!stack.isDamageableItem()) return true;

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return true;

        int currentDamage = stack.getDamageValue();
        int durabilityLeft = maxDamage - currentDamage;
        float percent = (float) durabilityLeft / (float) maxDamage;

        return percent >= 0.65F;
    }

    private void renderRank(GuiGraphics context, Slot slot, int rank) {
        String text = "#" + rank;

        var matrices = context.pose();
        matrices.pushMatrix();

        float scale = 0.8F;
        matrices.translate(slot.x, slot.y);
        matrices.scale(scale, scale);

        int color = switch (rank) {
            case 1 -> 0xFFFFD700;
            case 2 -> 0xFFC0C0C0;
            case 3 -> 0xFFCD7F32;
            default -> 0xFFFFFFFF;
        };

        context.drawString(mc.font, text, 1, 1, 0x44000000, false);
        context.drawString(mc.font, text, 0, 0, color, false);

        matrices.popMatrix();
    }

    private void renderPriceLabel(GuiGraphics context, Slot slot, int price, int textColor) {
        String text = formatPrice(price);

        var matrices = context.pose();
        matrices.pushMatrix();
        float scale = 0.78F;

        int textWidth = mc.font.width(text);
        int x = slot.x + 8 - (int) (textWidth * scale / 2);
        int y = slot.y + 17;

        matrices.translate(x, y);
        matrices.scale(scale, scale);

        context.fill(-1, -1, textWidth + 1, 9, 0xAA000000);
        context.drawString(mc.font, text, 0, 0, textColor, false);

        matrices.popMatrix();
    }

    private String formatPrice(int price) {
        if (price >= 1_000_000) {
            double kk = price / 1_000_000.0;

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setDecimalSeparator('.');

            DecimalFormat format = new DecimalFormat("#.###", symbols);
            return format.format(kk) + "KK";
        } else if (price >= 1_000) {
            return (price / 1_000) + "K";
        }

        return "$" + price;
    }

    private boolean isArmorGood(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;

        if (removeThorns.isValue() && getLevel(enchants, Enchantments.THORNS) > 0) {
            return false;
        }

        if (armorEnchants.isSelected("Защита 5") && getLevel(enchants, Enchantments.PROTECTION) < 5) {
            return false;
        }

        if (armorEnchants.isSelected("Починка") && getLevel(enchants, Enchantments.MENDING) < 1) {
            return false;
        }

        return true;
    }

    private boolean isSwordGood(ItemStack stack) {
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants == null) return false;

        if (removeKnockback.isValue() && getLevel(enchants, Enchantments.KNOCKBACK) > 0) {
            return false;
        }

        if (swordEnchants.isSelected("Острота")) {
            int level = getLevel(enchants, Enchantments.SHARPNESS);
            int required = getRequiredSharpness();
            if (required > 0 && level < required) return false;
            if (required == 0 && level < 1) return false;
        }

        if (swordEnchants.isSelected("Заговор огня") && getLevel(enchants, Enchantments.FIRE_ASPECT) < 2) {
            return false;
        }

        return true;
    }

    private int getRequiredSharpness() {
        return switch (sharpnessLevel.getSelected()) {
            case "Острота 5" -> 5;
            case "Острота 6" -> 6;
            case "Острота 7" -> 7;
            default -> 0;
        };
    }

    private boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = getItemId(stack);
        return id.contains("helmet") || id.contains("chestplate")
                || id.contains("leggings") || id.contains("boots");
    }

    private boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getItemId(stack).contains("sword");
    }

    private boolean isPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = getItemId(stack);
        return id.contains("potion");
    }

    private PotionMark getPotionMark(ItemStack stack) {
        if (!isPotion(stack)) return PotionMark.NONE;

        boolean checkStrength = potionEffects.isSelected("Сила III");
        boolean checkSpeed = potionEffects.isSelected("Скорость III");
        boolean checkIscel = potionEffects.isSelected("Исцеление II");

        boolean hasStrength3 = checkStrength && hasPotionEffectLevel(stack, MobEffects.STRENGTH, 3);
        boolean hasSpeed3 = checkSpeed && hasPotionEffectLevel(stack, MobEffects.SPEED, 3);
        boolean hasIscel2 = checkIscel && hasPotionEffectLevel(stack, MobEffects.INSTANT_HEALTH, 2);

        if (hasStrength3 && hasSpeed3) return PotionMark.BOTH_III;
        if (hasStrength3) return PotionMark.STRENGTH_III;
        if (hasSpeed3) return PotionMark.SPEED_III;
        if (hasIscel2) return PotionMark.HEAL_II;

        return PotionMark.NONE;
    }

    private boolean hasPotionEffectLevel(ItemStack stack, Holder<MobEffect> effect, int level) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;

        int neededAmplifier = level - 1;

        for (MobEffectInstance instance : contents.getAllEffects()) {
            Holder<MobEffect> instanceEffect = instance.getEffect();

            boolean matches = instanceEffect.is(effect)
                    || instanceEffect.equals(effect)
                    || instanceEffect.unwrapKey().equals(effect.unwrapKey());

            if (matches && instance.getAmplifier() == neededAmplifier) {
                return true;
            }
        }

        return false;
    }
    private enum PotionMark {
        NONE,
        STRENGTH_III,
        SPEED_III,
        HEAL_II,
        BOTH_III
    }

    private String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }


    private int getLevel(ItemEnchantments enchants, ResourceKey<Enchantment> key) {
        if (mc.level == null || enchants == null) return 0;

        var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var holder = lookup.get(key);

        if (holder.isEmpty()) return 0;

        return enchants.getLevel(holder.get());
    }

    private Slot findCheapest(List<Slot> slots, boolean perItem) {
        Slot cheapest = null;
        int lowest = Integer.MAX_VALUE;

        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !hasValidPrice(slot)) continue;

            if (!passesDurabilityCheck(stack)) continue;

            int price = perItem
                    ? FuntimeUtil.getPricePerItem(stack)
                    : FuntimeUtil.getPrice(stack);
            if (price < lowest) {
                lowest = price;
                cheapest = slot;
            }
        }

        return cheapest;
    }

    private int getBlinkingColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int alpha = 150;

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean hasValidPrice(Slot slot) {
        int price = FuntimeUtil.getPrice(slot.getItem());
        return price > 10 && price != 3;
    }

    private void highlightSlot(GuiGraphics context, Slot slot, int color) {
        if (slot != null) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
        }
    }
}