package ru.arixcompany.utils.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FallingPlayer {
    private final LocalPlayer player;
    private double x, y, z;
    private double motionX, motionY, motionZ;
    private final float yaw;
    private int simulatedTicks;

    // Константы физики
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.9800000190734863;

    public FallingPlayer(LocalPlayer player, double x, double y, double z, double motionX, double motionY, double motionZ, float yaw) {
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yaw = yaw;
        this.simulatedTicks = 0;
    }

    public static FallingPlayer fromPlayer(LocalPlayer player) {
        return new FallingPlayer(
                player,
                player.position().x,
                player.position().y,
                player.position().z,
                player.getDeltaMovement().x,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z,
                player.getYRot()
        );
    }

    /**
     * Проверяет, будет ли игрок падать с заданной скоростью в следующем тике.
     *
     * @param fallDist минимальная скорость падения для проверки (в блоках/тик)
     * @return true если |motionY| > fallDist
     */
    public boolean findFall(float fallDist) {
        // Текущая скорость падения (отрицательное значение)
        return Math.abs(motionY) > fallDist; // Нужен абсолютный модуль!
    }

    /**
     * Симулирует несколько тиков вперёд и проверяет, начнёт ли игрок падать.
     *
     * Формула применяется: motionY = (motionY - gravity) * drag
     *
     * @param fallDist минимальная скорость падения для срабатывания
     * @param ticks количество тиков для симуляции
     * @return true если в течение симуляции скорость падения превысит fallDist
     */
    public boolean findFall(float fallDist, int ticks) {
        double tempMotionY = motionY;

        // Базовая гравитация (без модификаторов)
        double gravity = GRAVITY;

        // Проверяем текущую скорость падения
        if (Math.abs(tempMotionY) > fallDist) {
            return true;
        }

        // Симулируем падение на несколько тиков
        for (int i = 0; i < ticks; i++) {
            // Физическая формула: v = (v - g) * drag
            // Гравитация замедляет игрока вниз, drag уменьшает ускорение
            tempMotionY = (tempMotionY - gravity) * DRAG;

            // Проверяем, достаточно ли быстро падаем
            if (Math.abs(tempMotionY) > fallDist) {
                return true;
            }
        }

        return false;
    }

    /**
     * Продвинутая версия с учётом модификаторов (Slow Falling, в воде/лаве)
     */
    public boolean findFallAdvanced(float fallDist, int ticks, boolean inLiquid, boolean hasSlowFalling) {
        double tempMotionY = motionY;

        double gravity = GRAVITY;
        double dragCoeff = DRAG;

        // В жидкости гравитация и сопротивление намного выше
        if (inLiquid) {
            gravity = 0.02; // меньше гравитация в воде
            dragCoeff = 0.8; // больше сопротивление
        }

        // Slow Falling ещё больше замедляет падение
        if (hasSlowFalling) {
            gravity = 0.01;
            dragCoeff = 0.9;
        }

        if (Math.abs(tempMotionY) > fallDist) {
            return true;
        }

        for (int i = 0; i < ticks; i++) {
            tempMotionY = (tempMotionY - gravity) * dragCoeff;

            if (Math.abs(tempMotionY) > fallDist) {
                return true;
            }
        }

        return false;
    }

    /**
     * Получить текущую скорость падения после симуляции N тиков
     */
    public double getMotionYAfterTicks(int ticks) {
        double tempMotionY = motionY;

        for (int i = 0; i < ticks; i++) {
            tempMotionY = (tempMotionY - GRAVITY) * DRAG;
        }

        return tempMotionY;
    }

    /**
     * Получить максимальную скорость падения которую достигнет игрок через N тиков
     */
    public double getMaxFallVelocityInTicks(int ticks) {
        double maxVelocity = Math.abs(motionY);
        double tempMotionY = motionY;

        for (int i = 0; i < ticks; i++) {
            tempMotionY = (tempMotionY - GRAVITY) * DRAG;
            maxVelocity = Math.max(maxVelocity, Math.abs(tempMotionY));
        }

        return maxVelocity;
    }
}