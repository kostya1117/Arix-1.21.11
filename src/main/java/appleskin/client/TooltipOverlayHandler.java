package appleskin.client;

import appleskin.ModConfig;
import appleskin.helpers.*;
import appleskin.helpers.TextureHelper.FoodType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;
import java.util.Optional;

public class TooltipOverlayHandler {

    public static TooltipOverlayHandler INSTANCE;

    public static void init() {
        INSTANCE = new TooltipOverlayHandler();
    }

    public static class FoodOverlay implements TooltipComponent, ClientTooltipComponent {
        public final FoodProperties defaultFood;
        public final FoodProperties modifiedFood;
        public final Consumable consumable;
        public final ItemStack itemStack;

        final int hungerBars;
        @Nullable final String hungerBarsText;
        final int saturationBars;
        @Nullable final String saturationBarsText;
        final int biggestHunger;
        final float biggestSaturation;

        public FoodOverlay(ItemStack stack, FoodProperties def, FoodProperties mod, Consumable consumable) {
            this.itemStack    = stack;
            this.defaultFood  = def;
            this.modifiedFood = mod;
            this.consumable   = consumable;

            biggestHunger     = Math.max(def.nutrition(), mod.nutrition());
            biggestSaturation = Math.max(def.saturation(), mod.saturation());

            int hb = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
            String hbt = null;
            if (hb > 10) { hbt = "x" + ((biggestHunger < 0 ? -1 : 1) * hb); hb = 1; }
            hungerBars     = hb;
            hungerBarsText = hbt;

            int sb = (int) Math.ceil(Math.abs(biggestSaturation) / 2f);
            String sbt = null;
            if (sb > 10 || sb == 0) { sbt = "x" + ((biggestSaturation < 0 ? -1 : 1) * sb); sb = 1; }
            saturationBars     = sb;
            saturationBarsText = sbt;
        }

        @Override
        public int getHeight(@NotNull Font font) { return 9 + 1 + 7 + 3; }

        @Override
        public int getWidth(@NotNull Font font) {
            int hl = hungerBars * 9 + (hungerBarsText != null ? font.width(hungerBarsText) : 0);
            int sl = saturationBars * 7 + (saturationBarsText != null ? font.width(saturationBarsText) : 0);
            return Math.max(hl, sl);
        }

        @Override
        public void renderImage(Font font, int x, int y, int vw, int vh, GuiGraphics ctx) {
            if (INSTANCE != null) INSTANCE.renderFoodOverlay(ctx, this, x, y, font);
        }
    }

    public Optional<TooltipComponent> getFoodTooltipImage(ItemStack stack) {
        if (ModConfig.INSTANCE == null || !shouldShowTooltip(stack)) return java.util.Optional.empty();
        FoodHelper.QueriedFoodResult result = FoodHelper.query(stack, null);
        if (result == null) return java.util.Optional.empty();
        FoodOverlay overlay = new FoodOverlay(stack,
                result.defaultFoodComponent, result.modifiedFoodComponent, result.consumableComponent);
        if (overlay.hungerBars <= 0) return Optional.empty();
        return Optional.of(overlay);
    }

    enum FoodOutline {
        NEGATIVE, EXTRA, NORMAL, PARTIAL, MISSING;

        public int argb() {
            return switch (this) {
                case NEGATIVE -> ColorHelper.argbFromRGBA(1f, 1f, 1f, 1f);
                case EXTRA    -> ColorHelper.argbFromRGBA(0.06f, 0.32f, 0.02f, 1f);
                case NORMAL   -> ColorHelper.argbFromRGBA(0f, 0f, 0f, 1f);
                case PARTIAL  -> ColorHelper.argbFromRGBA(0.53f, 0.21f, 0.08f, 1f);
                case MISSING  -> ColorHelper.argbFromRGBA(0.62f, 0f, 0f, 0.5f);
            };
        }

        public static FoodOutline get(int mod, int def, int i) {
            if (mod < 0)                          return NEGATIVE;
            if (mod > def && def <= i)            return EXTRA;
            if (mod > i + 1 || def == mod)        return NORMAL;
            if (mod == i + 1)                     return PARTIAL;
            return MISSING;
        }
    }

    public void renderFoodOverlay(GuiGraphics ctx, FoodOverlay overlay, int x, int y, Font font) {
        if (ctx == null || ModConfig.INSTANCE == null || overlay == null) return;

        Matrix3x2fStack mat = ctx.pose();
        int defHunger = overlay.defaultFood.nutrition();
        int modHunger = overlay.modifiedFood.nutrition();
        boolean rotten = FoodHelper.isRotten(overlay.consumable);

        int hx = x + (overlay.hungerBars - 1) * 9;
        for (int i = 0; i < overlay.hungerBars * 2; i += 2) {
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, hx, y, 9, 9);

            FoodOutline outline = FoodOutline.get(modHunger, defHunger, i);
            if (outline != FoodOutline.NORMAL) {
                ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, hx, y, 9, 9, outline.argb());
            }

            boolean defHalf = defHunger - 1 == i;
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED,
                    TextureHelper.getFoodTexture(rotten, defHalf ? FoodType.HALF : FoodType.FULL),
                    hx, y, 9, 9, ColorHelper.argbFromRGBA(1f, 1f, 1f, 0.25f));

            if (modHunger > i) {
                boolean modHalf = modHunger - 1 == i;
                ctx.blitSprite(RenderPipelines.GUI_TEXTURED,
                        TextureHelper.getFoodTexture(rotten, modHalf ? FoodType.HALF : FoodType.FULL),
                        hx, y, 9, 9);
            }
            hx -= 9;
        }
        if (overlay.hungerBarsText != null) {
            hx += 18;
            mat.pushMatrix(); mat.translate(hx, y); mat.scale(0.75f, 0.75f);
            ctx.drawString(font, overlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
            mat.popMatrix();
        }

        int sx = x + (overlay.saturationBars - 1) * 7;
        int sy = y + 10;
        float modSat = overlay.modifiedFood.saturation();
        float absSat = Math.abs(modSat);
        for (int i = 0; i < overlay.saturationBars * 2; i += 2) {
            float eff = (absSat - i) / 2f;
            boolean faded = absSat <= i;
            int color = faded ? ColorHelper.argbFromRGBA(1f, 1f, 1f, 0.5f) : ColorHelper.argbFromRGBA(1f, 1f, 1f, 1f);
            int u = eff >= 1 ? 21 : eff > 0.5f ? 14 : eff > 0.25f ? 7 : eff > 0 ? 0 : 28;
            int v = modSat >= 0 ? 27 : 34;
            ctx.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, sx, sy, u, v, 7, 7, 256, 256, color);
            sx -= 7;
        }
        if (overlay.saturationBarsText != null) {
            sx += 14;
            mat.pushMatrix(); mat.translate(sx, sy); mat.scale(0.75f, 0.75f);
            ctx.drawString(font, overlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
            mat.popMatrix();
        }
    }

    private boolean shouldShowTooltip(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!FoodHelper.isFood(stack)) return false;
        return (ModConfig.INSTANCE.showFoodValuesInTooltip && KeyHelper.isShiftKeyDown())
            || ModConfig.INSTANCE.showFoodValuesInTooltipAlways;
    }
}
