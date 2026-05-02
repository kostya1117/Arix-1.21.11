package com.mojang.blaze3d.vertex;

import net.optifine.Config;
import net.optifine.shaders.SVertexFormat;

public class DefaultVertexFormat {
    public static final VertexFormat EMPTY = VertexFormat.builder().build();
    public static final VertexFormat BLOCK = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV2", VertexFormatElement.UV2)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .build();
    public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV1", VertexFormatElement.UV1)
        .add("UV2", VertexFormatElement.UV2)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .build();
    public static final VertexFormat PARTICLE = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV2", VertexFormatElement.UV2)
        .build();
    public static final VertexFormat POSITION = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).build();
    public static final VertexFormat POSITION_COLOR = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    public static final VertexFormat POSITION_COLOR_NORMAL = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .build();
    public static final VertexFormat POSITION_COLOR_LIGHTMAP = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV2", VertexFormatElement.UV2)
        .build();
    public static final VertexFormat POSITION_TEX = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .build();
    public static final VertexFormat POSITION_TEX_COLOR = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV2", VertexFormatElement.UV2)
        .build();
    public static final VertexFormat POSITION_TEX_LIGHTMAP_COLOR = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV2", VertexFormatElement.UV2)
        .add("Color", VertexFormatElement.COLOR)
        .build();
    public static final VertexFormat POSITION_TEX_COLOR_NORMAL = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .build();
    public static final VertexFormat POSITION_COLOR_LINE_WIDTH = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("LineWidth", VertexFormatElement.LINE_WIDTH)
        .build();
    public static final VertexFormat POSITION_COLOR_NORMAL_LINE_WIDTH = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("Normal", VertexFormatElement.NORMAL)
        .add("LineWidth", VertexFormatElement.LINE_WIDTH)
        .build();
    public static final VertexFormat BLOCK_VANILLA = BLOCK.duplicate();
    public static final VertexFormat BLOCK_SHADERS = SVertexFormat.makeExtendedFormatBlock(BLOCK_VANILLA);
    public static final int BLOCK_VANILLA_SIZE = BLOCK_VANILLA.getVertexSize();
    public static final int BLOCK_SHADERS_SIZE = BLOCK_SHADERS.getVertexSize();
    public static final VertexFormat ENTITY_VANILLA = NEW_ENTITY.duplicate();
    public static final VertexFormat ENTITY_SHADERS = SVertexFormat.makeExtendedFormatEntity(ENTITY_VANILLA);
    public static final int ENTITY_VANILLA_SIZE = ENTITY_VANILLA.getVertexSize();
    public static final int ENTITY_SHADERS_SIZE = ENTITY_SHADERS.getVertexSize();

    public static void updateVertexFormats() {
        if (Config.isShaders()) {
            BLOCK.copyFrom(BLOCK_SHADERS);
            NEW_ENTITY.copyFrom(ENTITY_SHADERS);
        } else {
            BLOCK.copyFrom(BLOCK_VANILLA);
            NEW_ENTITY.copyFrom(ENTITY_VANILLA);
        }
    }

    static {
        BLOCK.setName("BLOCK");
        NEW_ENTITY.setName("ENTITY");
        PARTICLE.setName("PARTICLE_POSITION_TEX_COLOR_LMAP");
        POSITION.setName("POSITION");
        POSITION_COLOR.setName("POSITION_COLOR");
        POSITION_COLOR_NORMAL.setName("POSITION_COLOR_NORMAL");
        POSITION_COLOR_LIGHTMAP.setName("POSITION_COLOR_LIGHTMAP");
        POSITION_TEX.setName("POSITION_TEX");
        POSITION_TEX_COLOR.setName("POSITION_TEX_COLOR");
        POSITION_COLOR_TEX_LIGHTMAP.setName("POSITION_COLOR_TEX_LIGHTMAP");
        POSITION_TEX_LIGHTMAP_COLOR.setName("POSITION_TEX_LIGHTMAP_COLOR");
        POSITION_TEX_COLOR_NORMAL.setName("POSITION_TEX_COLOR_NORMAL");
        POSITION_COLOR_LINE_WIDTH.setName("POSITION_COLOR_LINE_WIDTH");
        POSITION_COLOR_NORMAL_LINE_WIDTH.setName("POSITION_COLOR_NORMAL_LINE_WIDTH");
        BLOCK_VANILLA.setName("BLOCK");
        ENTITY_VANILLA.setName("ENTITY");
        BLOCK_SHADERS.setName("BLOCK_SHADERS");
        ENTITY_SHADERS.setName("ENTITY_SHADERS");
    }
}
