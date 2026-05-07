package ru.arixcompany.features.module.modules.render;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;

public class FullBright extends Module {
    public FullBright() {
        super("FullBright", Category.Render);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (mc.player != null && mc.level != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @EventHandler
    public void onUpdate(EventTick e) {
        if (mc.player != null) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
        }
    }
}
