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

    /**
     * Проверяет GCD и выводит информацию в чат
     * Используй это для дебага
     */
    public void debugGCD() {
        double sensitivity = mc.options.sensitivity().get();
        double gcd = getGCD();
        float gcdValue = getGCDValue();
        
        MessageSender.print("§6=== GCD DEBUG ===");
        MessageSender.print("§eSensitivity: §f" + String.format("%.4f", sensitivity));
        MessageSender.print("§eGCD: §f" + String.format("%.6f", gcd));
        MessageSender.print("§eGCD Value (×0.15): §f" + String.format("%.6f", gcdValue));
        MessageSender.print("§eGCD is valid: §f" + (gcdValue > 0 && !Float.isNaN(gcdValue) && !Float.isInfinite(gcdValue)));
    }

    /**
     * Проверяет нормализацию ротации
     */
    public void debugRotationNormalization(Rotation target) {
        Rotation normalized = target.normalize();
        
        MessageSender.print("§6=== ROTATION NORMALIZATION DEBUG ===");
        MessageSender.print("§eOriginal: §fyaw=" + String.format("%.2f", target.yaw) + " pitch=" + String.format("%.2f", target.pitch));
        MessageSender.print("§eNormalized: §fyaw=" + String.format("%.2f", normalized.yaw) + " pitch=" + String.format("%.2f", normalized.pitch));
        MessageSender.print("§eIs Normalized: §f" + normalized.isNormalized);
        
        if (mc.player != null) {
            MessageSender.print("§ePlayer: §fyaw=" + String.format("%.2f", mc.player.getYRot()) + " pitch=" + String.format("%.2f", mc.player.getXRot()));
        }
    }

    /**
     * Проверяет дельту между двумя ротациями
     */
    public void debugRotationDelta(Rotation from, Rotation to) {
        Rotation.RotationDelta delta = from.rotationDeltaTo(to);
        float gcdValue = getGCDValue();
        
        float g1 = Math.round(delta.deltaYaw / gcdValue) * gcdValue;
        float g2 = Math.round(delta.deltaPitch / gcdValue) * gcdValue;
        
        MessageSender.print("§6=== ROTATION DELTA DEBUG ===");
        MessageSender.print("§eFrom: §fyaw=" + String.format("%.2f", from.yaw) + " pitch=" + String.format("%.2f", from.pitch));
        MessageSender.print("§eTo: §fyaw=" + String.format("%.2f", to.yaw) + " pitch=" + String.format("%.2f", to.pitch));
        MessageSender.print("§eDelta: §fyaw=" + String.format("%.2f", delta.deltaYaw) + " pitch=" + String.format("%.2f", delta.deltaPitch));
        MessageSender.print("§eGCD Value: §f" + String.format("%.6f", gcdValue));
        MessageSender.print("§eRounded to GCD: §fyaw=" + String.format("%.2f", g1) + " pitch=" + String.format("%.2f", g2));
        MessageSender.print("§eAngle between: §f" + String.format("%.2f", from.angleTo(to)));
    }

    /**
     * Полная проверка GCD системы
     */
    public void fullGCDCheck() {
        MessageSender.print("§6╔════════════════════════════════════╗");
        MessageSender.print("§6║     FULL GCD SYSTEM CHECK          ║");
        MessageSender.print("§6╚════════════════════════════════════╝");
        
        debugGCD();
        
        if (mc.player != null) {
            Rotation playerRot = new Rotation(mc.player);
            MessageSender.print("§ePlayer Rotation: §fyaw=" + String.format("%.2f", playerRot.yaw) + " pitch=" + String.format("%.2f", playerRot.pitch));
            
            // Тестируем нормализацию
            Rotation testRot = new Rotation(playerRot.yaw + 5, playerRot.pitch + 3);
            debugRotationNormalization(testRot);
        }
        
        MessageSender.print("§6════════════════════════════════════");
    }
}
