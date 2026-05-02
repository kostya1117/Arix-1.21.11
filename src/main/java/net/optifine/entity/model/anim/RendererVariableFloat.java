package net.optifine.entity.model.anim;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.optifine.render.RenderState;

public enum RendererVariableFloat implements IModelVariableFloat {
    SHADOW_SIZE("shadow_size"),
    SHADOW_OPACITY("shadow_opacity"),
    LEASH_OFFSET_X("leash_offset_x"),
    LEASH_OFFSET_Y("leash_offset_y"),
    LEASH_OFFSET_Z("leash_offset_z"),
    SHADOW_OFFSET_X("shadow_offset_x"),
    SHADOW_OFFSET_Z("shadow_offset_z");

    private String name;
    private static final RendererVariableFloat[] VALUES = values();

    RendererVariableFloat(String name) {
        this.name = name;
    }

    @Override
    public float eval() {
        return this.getValue();
    }

    @Override
    public float getValue() {
        EntityRenderer entityrenderer = RenderState.getEntityRenderer();
        if (entityrenderer == null) {
            return 0.0F;
        }

        switch (this) {
            case SHADOW_SIZE:
                return entityrenderer.shadowRadius;
            case SHADOW_OPACITY:
                return entityrenderer.shadowStrength;
            case LEASH_OFFSET_X:
            case LEASH_OFFSET_Y:
            case LEASH_OFFSET_Z:
            default:
                if (entityrenderer instanceof MobRenderer mobrenderer) {
                    switch (this) {
                        case LEASH_OFFSET_X:
                            return mobrenderer.leashOffsetX;
                        case LEASH_OFFSET_Y:
                            return mobrenderer.leashOffsetY;
                        case LEASH_OFFSET_Z:
                            return mobrenderer.leashOffsetZ;
                    }
                }

                return 0.0F;
            case SHADOW_OFFSET_X:
                return entityrenderer.shadowOffsetX;
            case SHADOW_OFFSET_Z:
                return entityrenderer.shadowOffsetZ;
        }
    }

    @Override
    public void setValue(float value) {
        EntityRenderer entityrenderer = RenderState.getEntityRenderer();
        if (entityrenderer != null) {
            switch (this) {
                case SHADOW_SIZE:
                    entityrenderer.shadowRadius = value;
                    return;
                case SHADOW_OPACITY:
                    entityrenderer.shadowStrength = value;
                    return;
                case LEASH_OFFSET_X:
                case LEASH_OFFSET_Y:
                case LEASH_OFFSET_Z:
                default:
                    if (entityrenderer instanceof MobRenderer mobrenderer) {
                        switch (this) {
                            case LEASH_OFFSET_X:
                                mobrenderer.leashOffsetX = value;
                                return;
                            case LEASH_OFFSET_Y:
                                mobrenderer.leashOffsetY = value;
                                return;
                            case LEASH_OFFSET_Z:
                                mobrenderer.leashOffsetZ = value;
                                return;
                        }
                    }

                    return;
                case SHADOW_OFFSET_X:
                    entityrenderer.shadowOffsetX = value;
                    return;
                case SHADOW_OFFSET_Z:
                    entityrenderer.shadowOffsetZ = value;
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public static RendererVariableFloat parse(String str) {
        if (str == null) {
            return null;
        }

        for (int i = 0; i < VALUES.length; i++) {
            RendererVariableFloat renderervariablefloat = VALUES[i];
            if (renderervariablefloat.getName().equals(str)) {
                return renderervariablefloat;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
