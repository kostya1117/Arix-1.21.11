package net.optifine.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.optifine.model.QuadBounds;

public class QuadProperties {
    public long[] packedUvsSingle = null;
    public QuadBounds quadBounds;
    public boolean quadEmissiveChecked;
    public BakedQuad quadEmissive;
    public QuadVertexPositions quadVertexPositions;
}
