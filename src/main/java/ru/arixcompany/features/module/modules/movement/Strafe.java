package ru.arixcompany.features.module.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.MoveUtils;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Strafe extends Module {

    final SelectSetting mode = new SelectSetting("Режим")
            .value("Default", "Matrix", "MetaHVH");

    final BooleanSetting damageBoost = new BooleanSetting("Буст при уроне")
            .setValue(false);

    final ValueSetting boostSpeed = new ValueSetting("Скорость буста")
            .setValue(0.7f)
            .range(0.1f, 5.0f)
            .step(0.1f)
            .visible(damageBoost::isValue);

    final Timer timer = new Timer();
    double prevSpeed = 0.0;

    public Strafe() {
        super("Strafe", Category.Movement);
        setup(mode, damageBoost, boostSpeed);
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        String m = mode.getSelected();

        if (!MoveUtils.isMoving()) {
            prevSpeed = 0;
            return;
        }

        double spd = getSpeed();

        if (damageBoost.isValue() && timer.finished(700L)) {
            spd += boostSpeed.getValue();
            timer.reset();
        }

        if (m.equals("Matrix")) {
            matrix(spd);
            return;
        }

        if (m.equals("MetaHVH")) {
            spd *= 1.2;
        }

        move(spd);
        prevSpeed = spd;
    }

    private void matrix(double spd) {
        double randomSpd = 0.25 - Math.random() * 0.001;
        move(randomSpd);
        prevSpeed = randomSpd;
    }

    private double getSpeed() {
        double spd = 0.2873;
        MobEffectInstance speedEffect = mc.player.getEffect(MobEffects.SPEED);
        if (speedEffect != null) {
            spd *= 1.0 + 0.2 * (speedEffect.getAmplifier() + 1);
        }
        return spd;
    }

    private void move(double spd) {
        float yaw = mc.player.getYRot();
        float fwd = getFwd();
        float str = getStr();

        if (fwd == 0 && str == 0) return;

        double radians = Math.toRadians(yaw);
        double x = (str * Math.cos(radians) - fwd * Math.sin(radians)) * spd;
        double z = (str * Math.sin(radians) + fwd * Math.cos(radians)) * spd;

        Vec3 motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(x, motion.y, z);
    }

    private float getFwd() {
        return (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
    }

    private float getStr() {
        return (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
    }
}
