package ru.arixcompany.features.module.modules.movement;

import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.player.MoveUtils;

public class Strafe extends Module {
    private final ValueSetting speed = new ValueSetting("Сила")
            .range(1f, 1.1f)
            .setValue(1f)
            .setStep(0.01f);

    private final BooleanSetting damageBoost = new BooleanSetting("Буст при уроне")
            .setValue(false);

    private final ValueSetting boostStrength = new ValueSetting("Сила буста")
            .range(1f, 1.1f)
            .setValue(1.15f)
            .setStep(0.01f)
            .visible(damageBoost::isValue);

    private int lastHurtTime = 0;
    private boolean boosting = false;
    private int boostTicks = 0;
    private static final int BOOST_DURATION = 6;

    public Strafe() {
        super("Strafe", Category.Movement);
        setup(speed, damageBoost, boostStrength);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        lastHurtTime = 0;
        boosting = false;
        boostTicks = 0;
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        int currentHurtTime = mc.player.hurtTime;
        if (damageBoost.isValue() && currentHurtTime > lastHurtTime && currentHurtTime == 10) {
            boosting = true;
            boostTicks = BOOST_DURATION;
        }
        lastHurtTime = currentHurtTime;

        if (boosting) {
            boostTicks--;
            if (boostTicks <= 0) boosting = false;
        }

        if (!MoveUtils.isMoving()) return;

        Vec3 motion = mc.player.getDeltaMovement();
        double currentPlayerSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double yaw = Math.toRadians(MoveUtils.getPlayerDirection());

        double multiplier = boosting ? boostStrength.getValue() : speed.getValue();
        double newSpeed = currentPlayerSpeed * multiplier;

        mc.player.setDeltaMovement(
                -Math.sin(yaw) * newSpeed,
                motion.y,
                Math.cos(yaw) * newSpeed
        );
    }
}
