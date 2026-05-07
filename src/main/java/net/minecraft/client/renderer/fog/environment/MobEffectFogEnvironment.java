package net.minecraft.client.renderer.fog.environment;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.jspecify.annotations.Nullable;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.NoRender;

public abstract class MobEffectFogEnvironment extends FogEnvironment {
    public abstract Holder<MobEffect> getMobEffect();

    @Override
    public boolean providesColor() {
        return false;
    }

    @Override
    public boolean modifiesDarkness() {
        return true;
    }

    @Override
    public boolean isApplicable(FogType p_409479_, Entity p_410566_) {
        boolean original = p_410566_ instanceof LivingEntity livingentity && livingentity.hasEffect(this.getMobEffect());

        NoRender mod = (NoRender) Arix.getInstance().getModuleRepo().getModule(NoRender.class);

        if (mod == null) return original;

        if (this.getMobEffect() == MobEffects.BLINDNESS || this.getMobEffect() == MobEffects.DARKNESS) {
            return original && !mod.noBadEffects();
        }

        return original;
    }
}