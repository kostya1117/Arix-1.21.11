package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import ru.arixcompany.utils.IMinecraft;

public class SensUtils implements IMinecraft {

    public static float getSensitivity(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public static float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public static double getGCD() {
        double d2 = mc.options.sensitivity().get() * 0.6F + 0.2F;
        double d3 = d2 * d2 * d2;
        return d3 * 8.0;
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    public static float applyMinimalThreshold(float delta, float threshold) {
        return Math.abs(delta) < threshold ? 0 : delta;
    }
}