package appleskin.client;

import appleskin.ModConfig;
import appleskin.helpers.*;
import appleskin.helpers.TextureHelper.FoodType;
import appleskin.helpers.TextureHelper.HeartType;
import appleskin.util.IntPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.Vector;

public class HUDOverlayHandler {

    public static HUDOverlayHandler INSTANCE;

    private float unclampedFlashAlpha = 0f;
    private float flashAlpha = 0f;
    private byte alphaDir = 1;

    public final OffsetsCache barOffsets = new OffsetsCache();
    public final HeldFoodCache heldFood  = new HeldFoodCache();

    public static void init() {
        INSTANCE = new HUDOverlayHandler();
    }

    public void onPreRenderFood(GuiGraphics context, Player player, int top, int right) {
        if (ModConfig.INSTANCE == null || !ModConfig.INSTANCE.showFoodExhaustionHudUnderlay) return;
        float exhaustion = ExhaustionHelper.getExhaustion(player);
        drawExhaustionOverlay(context, exhaustion, right, top, 1f);
    }

    public void onRenderFood(GuiGraphics context, Player player, int top, int right) {
        if (ModConfig.INSTANCE == null) return;
        if (!shouldRenderAnyOverlays()) return;

        Minecraft mc = Minecraft.getInstance();
        FoodData stats = player.getFoodData();

        if (ModConfig.INSTANCE.showSaturationHudOverlay) {
            drawSaturationOverlay(context, 0, stats.getSaturationLevel(), mc, right, top, 1f, mc.gui.getGuiTicks());
        }

        FoodHelper.QueriedFoodResult result = heldFood.result(mc.gui.getGuiTicks(), player);
        if (result == null) { resetFlash(); return; }

        if (ModConfig.INSTANCE.showFoodValuesHudOverlay) {
            int foodHunger = result.modifiedFoodComponent.nutrition();
            float foodSat   = result.modifiedFoodComponent.saturation();

            drawHungerOverlay(context, foodHunger, stats.getFoodLevel(), mc, right, top, flashAlpha,
                    FoodHelper.isRotten(result.consumableComponent), mc.gui.getGuiTicks());

            if (ModConfig.INSTANCE.showSaturationHudOverlay) {
                int newFood = stats.getFoodLevel() + foodHunger;
                float newSat = stats.getSaturationLevel() + foodSat;
                float gained = newSat > newFood ? newFood - stats.getSaturationLevel() : foodSat;
                drawSaturationOverlay(context, gained, stats.getSaturationLevel(), mc, right, top, flashAlpha, mc.gui.getGuiTicks());
            }
        }
    }

    public void onRenderHealth(GuiGraphics context, Player player, int left, int top) {
        if (ModConfig.INSTANCE == null) return;
        if (!shouldRenderAnyOverlays()) return;

        Minecraft mc = Minecraft.getInstance();
        FoodHelper.QueriedFoodResult result = heldFood.result(mc.gui.getGuiTicks(), player);
        if (result == null) { resetFlash(); return; }

        if (shouldShowEstimatedHealth(player, mc.gui.getGuiTicks())) {
            float foodHealth = FoodHelper.getEstimatedHealthIncrement(player,
                    new ConsumableFood(result.modifiedFoodComponent, result.consumableComponent));
            float current  = player.getHealth();
            float modified = Math.min(current + foodHealth, player.getMaxHealth());
            if (current < modified) {
                drawHealthOverlay(context, current, modified, mc, left, top, flashAlpha, mc.gui.getGuiTicks());
            }
        }
    }

    public void onClientTick() {
        unclampedFlashAlpha += alphaDir * 0.125f;
        if (unclampedFlashAlpha >= 1.5f)       alphaDir = -1;
        else if (unclampedFlashAlpha <= -0.5f) alphaDir =  1;
        flashAlpha = Math.max(0f, Math.min(1f, unclampedFlashAlpha))
                   * Math.max(0f, Math.min(1f, ModConfig.INSTANCE.maxHudOverlayFlashAlpha));
    }

    public void resetFlash() {
        unclampedFlashAlpha = flashAlpha = 0f;
        alphaDir = 1;
    }

    public void drawSaturationOverlay(GuiGraphics ctx, float satGained, float satLevel,
                                       Minecraft mc, int right, int top, float alpha, int guiTicks) {
        if (satLevel + satGained < 0) return;
        int alphaColor = ColorHelper.argbFromRGBA(1f, 1f, 1f, alpha);
        float modSat = Math.max(0, Math.min(satLevel + satGained, 20));
        int start = satGained != 0 ? (int) Math.max(satLevel / 2f, 0) : 0;
        int end   = (int) Math.ceil(modSat / 2f);
        int iconSize = 9;
        var offsets = barOffsets.foodBarOffsets(guiTicks, mc.player);
        for (int i = start; i < end; i++) {
            IntPoint off = i < offsets.size() ? offsets.get(i) : new IntPoint();
            if (off == null) continue;
            int x = right + off.x, y = top + off.y;
            float eff = (modSat / 2f) - i;
            int u = eff >= 1 ? 27 : eff > .5f ? 18 : eff > .25f ? 9 : 0;
            ctx.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, u, 0, iconSize, iconSize, 256, 256, alphaColor);
        }
    }

    public void drawHungerOverlay(GuiGraphics ctx, int hungerRestored, int foodLevel,
                                   Minecraft mc, int right, int top, float alpha,
                                   boolean rotten, int guiTicks) {
        if (hungerRestored <= 0) return;
        int alphaColor = ColorHelper.argbFromRGBA(1f, 1f, 1f, alpha);
        int modFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));
        int start = Math.max(0, foodLevel / 2);
        int end   = (int) Math.ceil(modFood / 2f);
        int iconSize = 9;
        var offsets = barOffsets.foodBarOffsets(guiTicks, mc.player);
        for (int i = start; i < end; i++) {
            IntPoint off = i < offsets.size() ? offsets.get(i) : new IntPoint();
            if (off == null) continue;
            int x = right + off.x, y = top + off.y;
            int bgColor = ColorHelper.argbFromRGBA(1f, 1f, 1f, alpha * 0.25f);
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.getFoodTexture(rotten, FoodType.EMPTY), x, y, iconSize, iconSize, bgColor);
            boolean half = i * 2 + 1 == modFood;
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.getFoodTexture(rotten, half ? FoodType.HALF : FoodType.FULL), x, y, iconSize, iconSize, alphaColor);
        }
    }

    public void drawHealthOverlay(GuiGraphics ctx, float health, float modHealth,
                                   Minecraft mc, int left, int top, float alpha, int guiTicks) {
        if (modHealth <= health) return;
        int alphaColor = ColorHelper.argbFromRGBA(1f, 1f, 1f, alpha);
        int fixedMod = (int) Math.ceil(modHealth);
        boolean hardcore = mc.player.level().getLevelData().isHardcore();
        int start = (int) Math.max(0, Math.ceil(health) / 2f);
        int end   = (int) Math.max(0, Math.ceil(modHealth / 2f));
        int iconSize = 9;
        var offsets = barOffsets.healthBarOffsets(guiTicks, mc.player);
        for (int i = start; i < end; i++) {
            IntPoint off = i < offsets.size() ? offsets.get(i) : new IntPoint();
            if (off == null) continue;
            int x = left + off.x, y = top + off.y;
            int bgColor = ColorHelper.argbFromRGBA(1f, 1f, 1f, alpha * 0.25f);
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.getHeartTexture(hardcore, HeartType.CONTAINER), x, y, iconSize, iconSize, bgColor);
            boolean half = i * 2 + 1 == fixedMod;
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.getHeartTexture(hardcore, half ? HeartType.HALF : HeartType.FULL), x, y, iconSize, iconSize, alphaColor);
        }
    }

    public void drawExhaustionOverlay(GuiGraphics ctx, float exhaustion, int right, int top, float alpha) {
        float ratio = Math.min(1f, Math.max(0f, exhaustion / FoodHelper.MAX_EXHAUSTION));
        int width = (int)(ratio * 81);
        int height = 9;
        int color = ColorHelper.argbFromRGBA(1f, 1f, 1f, 0.75f);
        ctx.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, right - width, top, 81 - width, 18, width, height, 256, 256, color);
    }

    private boolean shouldRenderAnyOverlays() {
        return ModConfig.INSTANCE.showFoodValuesHudOverlay
            || ModConfig.INSTANCE.showSaturationHudOverlay
            || ModConfig.INSTANCE.showFoodHealthHudOverlay;
    }

    private boolean shouldShowEstimatedHealth(Player player, int guiTicks) {
        if (!ModConfig.INSTANCE.showFoodHealthHudOverlay) return false;
        if (barOffsets.healthBarOffsets(guiTicks, player).isEmpty()) return false;
        FoodData stats = player.getFoodData();
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) return false;
        if (stats.getFoodLevel() >= 18) return false;
        return !player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
            && !player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)
            && !player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
    }

    public static class OffsetsCache {
        protected final Vector<IntPoint> foodBarOffsets   = new Vector<>();
        protected final Vector<IntPoint> healthBarOffsets = new Vector<>();
        public int lastGuiTick = 0;
        protected final Random random = new Random();

        protected void generate(int guiTicks, Player player) {
            final int preferHealth = 10, preferFood = 10;
            float maxHealth = player.getMaxHealth();
            float absorption = (float) Math.ceil(player.getAbsorptionAmount());
            int healthBars = (int) Math.ceil((maxHealth + absorption) / 2f);
            if (healthBars < 0 || healthBars > 1000) healthBars = 0;
            int healthRows = (int) Math.ceil((float) healthBars / preferHealth);
            int healthRowH = Math.max(10 - (healthRows - 2), 3);

            boolean animFood = false, animHealth = false;
            if (ModConfig.INSTANCE.showVanillaAnimationsOverlay) {
                FoodData hm = player.getFoodData();
                float sat = hm.getSaturationLevel();
                int food = hm.getFoodLevel();
                animFood   = sat <= 0f && guiTicks % (food * 3 + 1) == 0;
                animHealth = Math.ceil(player.getHealth()) <= 4;
            }

            random.setSeed((long)(guiTicks * 312871));

            if (healthBarOffsets.size() != healthBars) healthBarOffsets.setSize(healthBars);
            if (foodBarOffsets.size()   != preferFood)  foodBarOffsets.setSize(preferFood);

            for (int i = healthBars - 1; i >= 0; i--) {
                int row = (int) Math.ceil((float)(i + 1) / preferHealth) - 1;
                int x = i % preferHealth * 8;
                int y = -(row * healthRowH);
                if (animHealth) y += random.nextInt(2);
                IntPoint p = healthBarOffsets.get(i);
                if (p == null) { p = new IntPoint(); healthBarOffsets.set(i, p); }
                p.x = x; p.y = y;
            }

            for (int i = 0; i < preferFood; i++) {
                int x = -(i * 8) - 9;
                int y = 0;
                if (animFood) y += random.nextInt(3) - 1;
                IntPoint p = foodBarOffsets.get(i);
                if (p == null) { p = new IntPoint(); foodBarOffsets.set(i, p); }
                p.x = x; p.y = y;
            }
        }

        public Vector<IntPoint> healthBarOffsets(int tick, Player player) {
            if (tick != lastGuiTick) { generate(tick, player); lastGuiTick = tick; }
            return healthBarOffsets;
        }

        public Vector<IntPoint> foodBarOffsets(int tick, Player player) {
            if (tick != lastGuiTick) { generate(tick, player); lastGuiTick = tick; }
            return foodBarOffsets;
        }
    }

    public static class HeldFoodCache {
        @Nullable protected FoodHelper.QueriedFoodResult result;
        public int lastGuiTick = 0;

        protected void query(Player player) {
            ItemStack held = player.getMainHandItem();
            FoodHelper.QueriedFoodResult food = FoodHelper.query(held, player);
            boolean canConsume = food != null && FoodHelper.canConsume(player, food.modifiedFoodComponent);
            if (ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand && !canConsume) {
                held = player.getOffhandItem();
                food = FoodHelper.query(held, player);
                canConsume = food != null && FoodHelper.canConsume(player, food.modifiedFoodComponent);
            }
            this.result = (!held.isEmpty() && canConsume) ? food : null;
        }

        public FoodHelper.QueriedFoodResult result(int tick, Player player) {
            if (tick != lastGuiTick) { query(player); lastGuiTick = tick; }
            return result;
        }
    }
}
