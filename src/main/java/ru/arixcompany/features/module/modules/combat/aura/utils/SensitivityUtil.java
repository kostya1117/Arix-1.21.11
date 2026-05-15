package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;

@UtilityClass
public class SensitivityUtil implements IMinecraft {
    public float getSensitivity(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    public float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public double getGCD() {
        double sens = mc.options.sensitivity().get() * 0.6 + 0.2;
        return sens * sens * sens * 8;
    }
}
