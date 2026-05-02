package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.RenderTypeGroup;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public interface ModelBaker {
    ResolvedModel getModel(Identifier p_456994_);

    BlockModelPart missingBlockModelPart();

    SpriteGetter sprites();

    ModelBaker.PartCache parts();

    <T> T compute(ModelBaker.SharedOperationKey<T> p_395456_);

    default RenderTypeGroup renderType() {
        return null;
    }

    default RenderTypeGroup renderTypeFast() {
        return null;
    }

    interface PartCache {
        default Vector3fc vector(float p_452065_, float p_451254_, float p_452365_) {
            return this.vector(new Vector3f(p_452065_, p_451254_, p_452365_));
        }

        Vector3fc vector(Vector3fc p_460548_);
    }

    @FunctionalInterface
    interface SharedOperationKey<T> {
        T compute(ModelBaker p_393089_);
    }
}
