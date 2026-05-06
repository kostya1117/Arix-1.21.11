package ru.arixcompany.features.module.modules.combat.aura.rotation;

import net.minecraft.world.entity.LivingEntity;

public interface AbstractRotation {
    default void rotate(LivingEntity target, boolean attack) {}
    default void rotate(LivingEntity target, boolean attack, float dist, boolean check) {}
}