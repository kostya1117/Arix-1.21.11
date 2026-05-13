package appleskin.helpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.jetbrains.annotations.Nullable;

public class FoodHelper {

    public static final float MAX_EXHAUSTION = 4.0f;
    public static final float REGEN_EXHAUSTION_INCREMENT = 6.0f;

    public static boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE);
    }

    public static boolean canConsume(Player player, FoodProperties food) {
        return player.canEat(food.canAlwaysEat());
    }

    public static ConsumableFood getDefaultFoodValues(ItemStack stack) {
        FoodProperties food = stack.getOrDefault(DataComponents.FOOD,
                new FoodProperties(0, 0f, false));
        Consumable consumable = stack.getOrDefault(DataComponents.CONSUMABLE,
                Consumable.builder().build());
        return new ConsumableFood(food, consumable);
    }

    public static class QueriedFoodResult {
        public FoodProperties defaultFoodComponent;
        public FoodProperties modifiedFoodComponent;
        public Consumable consumableComponent;
        public final ItemStack itemStack;

        public QueriedFoodResult(FoodProperties def, FoodProperties mod, Consumable consumable, ItemStack stack) {
            this.defaultFoodComponent = def;
            this.modifiedFoodComponent = mod;
            this.consumableComponent = consumable;
            this.itemStack = stack;
        }
    }

    @Nullable
    public static QueriedFoodResult query(ItemStack stack, @Nullable Player player) {
        if (!isFood(stack)) return null;
        ConsumableFood def = getDefaultFoodValues(stack);
        return new QueriedFoodResult(def.food(), def.food(), def.consumable(), stack);
    }

    public static boolean isRotten(Consumable consumable) {
        for (var effect : consumable.onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect applyEffect)) continue;
            for (var instance : applyEffect.effects()) {
                var v = instance.getEffect().value();
                if (v == MobEffects.POISON.value() || v == MobEffects.WITHER.value()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float getEstimatedHealthIncrement(Player player, ConsumableFood food) {
        if (player.getHealth() >= player.getMaxHealth()) return 0f;

        FoodData stats = player.getFoodData();
        int foodLevel = Math.min(stats.getFoodLevel() + food.food().nutrition(), 20);
        float healthIncrement = 0f;

        if (foodLevel >= 18) {
            float saturation = Math.min(stats.getSaturationLevel() + food.food().saturation(), foodLevel);
            float exhaustion = ExhaustionHelper.getExhaustion(player);
            healthIncrement = getEstimatedHealthIncrement(foodLevel, saturation, exhaustion);
        }

        for (var effect : food.consumable().onConsumeEffects()) {
            if (!(effect instanceof ApplyStatusEffectsConsumeEffect applyEffect)) continue;
            for (var instance : applyEffect.effects()) {
                if (instance.getEffect().value() == MobEffects.REGENERATION.value()) {
                    int amp = instance.getAmplifier();
                    int dur = instance.getDuration();
                    healthIncrement += (float) Math.floor(dur / Math.max(50 >> amp, 1));
                    break;
                }
            }
        }

        return healthIncrement;
    }

    public static float getEstimatedHealthIncrement(int foodLevel, float saturation, float exhaustion) {
        float health = 0f;

        if (!Float.isFinite(exhaustion) || !Float.isFinite(saturation)) return 0f;

        while (foodLevel >= 18) {
            while (exhaustion > MAX_EXHAUSTION) {
                exhaustion -= MAX_EXHAUSTION;
                if (saturation > 0f)
                    saturation = Math.max(saturation - 1f, 0f);
                else
                    foodLevel -= 1;
            }
            if (foodLevel >= 20 && Float.compare(saturation, Float.MIN_NORMAL) > 0) {
                float limited = Math.min(saturation, REGEN_EXHAUSTION_INCREMENT);
                float until = Math.nextUp(MAX_EXHAUSTION) - exhaustion;
                int iters = Math.max(1, (int) Math.ceil(until / limited));
                health += (limited / REGEN_EXHAUSTION_INCREMENT) * iters;
                exhaustion += limited * iters;
            } else if (foodLevel >= 18) {
                health += 1f;
                exhaustion += REGEN_EXHAUSTION_INCREMENT;
            }
        }
        return health;
    }
}
