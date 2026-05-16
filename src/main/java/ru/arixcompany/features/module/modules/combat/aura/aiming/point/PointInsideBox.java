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
package ru.arixcompany.features.module.modules.combat.aura.aiming.point;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record PointInsideBox(Vec3 pos, AABB box) {

    /**
     * Creates a PointInsideBox, clamping pos to the nearest point on the box.
     */
    public static PointInsideBox of(Vec3 pos, AABB box) {
        Vec3 nearest = new Vec3(
            Mth.clamp(pos.x, box.minX, box.maxX),
            Mth.clamp(pos.y, box.minY, box.maxY),
            Mth.clamp(pos.z, box.minZ, box.maxZ)
        );
        return new PointInsideBox(nearest, box);
    }

    public double distanceTo(PointInsideBox other) {
        return pos.distanceTo(other.pos);
    }

    public double distanceTo(Vec3 point) {
        return pos.distanceTo(point);
    }

    public double distanceToSqr(PointInsideBox other) {
        return pos.distanceToSqr(other.pos);
    }

    public double distanceToSqr(Vec3 point) {
        return pos.distanceToSqr(point);
    }

    public PointInsideBox add(Vec3 offset) {
        return new PointInsideBox(pos.add(offset), new AABB(
            box.minX + offset.x, box.minY + offset.y, box.minZ + offset.z,
            box.maxX + offset.x, box.maxY + offset.y, box.maxZ + offset.z
        ));
    }
}
