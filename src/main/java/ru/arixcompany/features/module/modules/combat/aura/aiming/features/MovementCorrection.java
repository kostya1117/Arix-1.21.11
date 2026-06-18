/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura.aiming.features;

/**
 * Corrects movement when aiming away from client-side view direction.
 */
public enum MovementCorrection {

    /**
     * No movement correction. Does not change movement or sprinting.
     * Can be detected by anti-cheats.
     */
    OFF,

    /**
     * Correct movement by changing the yaw when updating the movement input,
     * while also smoothing the keyboard input to avoid aggressive walk direction changes.
     */
    SILENT,

    /**
     * Corrects movement by changing the actual look direction of the player.
     * Requires a render handler to smoothly interpolate the visual rotation.
     */
    CHANGE_LOOK

}
