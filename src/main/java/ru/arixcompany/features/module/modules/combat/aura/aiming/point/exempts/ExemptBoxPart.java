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
package ru.arixcompany.features.module.modules.combat.aura.aiming.point.exempts;

import net.minecraft.world.phys.Vec3;

public enum ExemptBoxPart implements ExemptPoint {

    HEAD {
        @Override
        public boolean predicate(ExemptContext context, Vec3 point) {
            double length = context.box().getYsize() / 3.0;
            return point.y <= context.box().maxY &&
                   point.y > context.box().maxY - length;
        }
    },
    BODY {
        @Override
        public boolean predicate(ExemptContext context, Vec3 point) {
            double length = context.box().getYsize() / 3.0;
            return point.y <= context.box().maxY - length &&
                   point.y >= context.box().minY + length;
        }
    },
    FEET {
        @Override
        public boolean predicate(ExemptContext context, Vec3 point) {
            double length = context.box().getYsize() / 3.0;
            return point.y >= context.box().minY &&
                   point.y < context.box().minY + length;
        }
    };

    /**
     * Check if this part of the box is higher than the other by the index of the enum.
     * So please DO NOT change the order of the enum.
     */
    public boolean isHigherThan(ExemptBoxPart other) {
        return ordinal() < other.ordinal();
    }
}
