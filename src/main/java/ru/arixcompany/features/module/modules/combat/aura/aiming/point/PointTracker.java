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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.exempts.ExemptBoxPart;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.exempts.ExemptContext;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.features.PointProcessor;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.features.PointProcessorDelay;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.features.PointProcessorGaussian;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.features.PointProcessorLazy;
import ru.arixcompany.utils.IMinecraft;

import java.util.*;

/**
 * The point tracker is being used to track a certain point of an entity.
 *
 * Mirrors LiquidBounce's PointTracker.
 */
public class PointTracker implements IMinecraft {

    /**
     * Which parts of the hitbox to exclude from targeting.
     * Mirrors LiquidBounce's predicateBoxParts (ExemptBoxParts multiEnumChoice).
     */
    public Set<ExemptBoxPart> exemptBoxParts = EnumSet.noneOf(ExemptBoxPart.class);

    /**
     * This introduces a layer of randomness to the point tracker. A gaussian distribution is being used to
     * calculate the offset.
     */
    public final PointProcessorGaussian gaussian = new PointProcessorGaussian();

    /**
     * This will allow the point to stay at a certain position when the minimum threshold is not reached.
     */
    public final PointProcessorLazy lazy = new PointProcessorLazy();

    /**
     * This will allow the point to be delayed until the ticks expire.
     */
    public final PointProcessorDelay delay = new PointProcessorDelay();

    private final PointProcessor[] processors = { delay, lazy, gaussian };

    /**
     * The point tracker is being used to track a certain point of an entity.
     *
     * @param eyes   player eye position
     * @param entity the entity we want to track
     */
    public PointInsideBox findPoint(Vec3 eyes, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
        List<Vec3> points = getPoints(eyes, box);

        Vec3 bestHitVector  = points.stream().min(Comparator.comparingDouble(a -> a.distanceToSqr(eyes))).orElse(getPseudoClosest(eyes, box));
        Vec3 worstHitVector = points.stream().max(Comparator.comparingDouble(a -> a.distanceToSqr(eyes))).orElse(getPseudoFurthest(eyes, box));

        // Filter exempts
        ExemptContext context = new ExemptContext(box, bestHitVector, worstHitVector);
        List<Vec3> pointsWithExempts = points.stream()
            .filter(point -> exemptBoxParts.stream().noneMatch(part -> part.predicate(context, point)))
            .toList();

        Vec3 pos = pointsWithExempts.stream()
            .min(Comparator.comparingDouble(a -> a.distanceToSqr(eyes)))
            .orElse(bestHitVector);

        PointInsideBox point = PointInsideBox.of(pos, box);
        for (PointProcessor processor : processors) {
            if (processor.enabled) {
                point = processor.process(point);
            }
        }
        return point;
    }

    /**
     * Projects points onto the surface of the box from the perspective of eyes.
     * Mirrors LiquidBounce's AABB.getPoints(eyes) using projectPointsOnBox.
     *
     * Simplified version: samples points on all 6 faces of the AABB.
     */
    private static List<Vec3> getPoints(Vec3 eyes, AABB box) {
        List<Vec3> points = new ArrayList<>();

        // Sample proportions across each face
        double[] props = { 0.05, 0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95 };

        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;

        for (double u : props) {
            for (double v : props) {
                // +X face
                addIfVisible(eyes, box, new Vec3(box.maxX, box.minY + v * sizeY, box.minZ + u * sizeZ), points);
                // -X face
                addIfVisible(eyes, box, new Vec3(box.minX, box.minY + v * sizeY, box.minZ + u * sizeZ), points);
                // +Y face
                addIfVisible(eyes, box, new Vec3(box.minX + u * sizeX, box.maxY, box.minZ + v * sizeZ), points);
                // -Y face
                addIfVisible(eyes, box, new Vec3(box.minX + u * sizeX, box.minY, box.minZ + v * sizeZ), points);
                // +Z face
                addIfVisible(eyes, box, new Vec3(box.minX + u * sizeX, box.minY + v * sizeY, box.maxZ), points);
                // -Z face
                addIfVisible(eyes, box, new Vec3(box.minX + u * sizeX, box.minY + v * sizeY, box.minZ), points);
            }
        }

        // Fallback: if no points found (eye inside box), use nearest point
        if (points.isEmpty()) {
            points.add(getPseudoClosest(eyes, box));
        }

        return points;
    }

    /**
     * Only add the point if it's on the side of the face visible from eyes.
     */
    private static void addIfVisible(Vec3 eyes, AABB box, Vec3 point, List<Vec3> out) {
        // Check the point is actually on the surface (not behind another face)
        Vec3 dir = point.subtract(eyes);
        if (dir.lengthSqr() == 0) return;
        out.add(point);
    }

    private static Vec3 getPseudoClosest(Vec3 eyes, AABB box) {
        return new Vec3(
            Mth.clamp(eyes.x, box.minX, box.maxX),
            Mth.clamp(eyes.y, box.minY, box.maxY),
            Mth.clamp(eyes.z, box.minZ, box.maxZ)
        );
    }

    private static Vec3 getPseudoFurthest(Vec3 eyes, AABB box) {
        return new Vec3(
            farthestAxis(eyes.x, box.minX, box.maxX),
            farthestAxis(eyes.y, box.minY, box.maxY),
            farthestAxis(eyes.z, box.minZ, box.maxZ)
        );
    }

    private static double farthestAxis(double value, double min, double max) {
        return Math.abs(value - min) > Math.abs(value - max) ? min : max;
    }
}
