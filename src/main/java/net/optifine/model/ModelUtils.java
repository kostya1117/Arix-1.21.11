package net.optifine.model;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.Direction;
import net.optifine.Config;
import org.joml.Vector3fc;

public class ModelUtils {
    public ModelUtils() {
    }

    public static void dbgModel(BlockModelPart model) {
        if (model != null) {
            String var10000 = String.valueOf(model);
            Config.dbg("Model: " + var10000 + ", ao: " + model.useAmbientOcclusion() + ", particle: " + String.valueOf(model.particleIcon()));
            Direction[] faces = Direction.VALUES;

            for(int i = 0; i < faces.length; ++i) {
                Direction face = faces[i];
                List faceQuads = model.getQuads(face);
                dbgQuads(face.getSerializedName(), faceQuads, "  ");
            }

            List generalQuads = model.getQuads((Direction)null);
            dbgQuads("General", generalQuads, "  ");
        }
    }

    private static void dbgQuads(String name, List quads, String prefix) {
        Iterator it = quads.iterator();

        while(it.hasNext()) {
            BakedQuad quad = (BakedQuad)it.next();
            dbgQuad(name, quad, prefix);
        }

    }

    public static void dbgQuad(String name, BakedQuad quad, String prefix) {
        Config.dbg(prefix + "Quad: " + quad.getClass().getName() + ", type: " + name + ", face: " + String.valueOf(quad.direction()) + ", tint: " + quad.tintIndex() + ", sprite: " + String.valueOf(quad.sprite()));
        dbgVertexData(quad, "  " + prefix);
    }

    public static void dbgVertexData(BakedQuad quad, String prefix) {
        for(int i = 0; i < 4; ++i) {
            Vector3fc pos = quad.position(i);
            float x = pos.x();
            float y = pos.y();
            float z = pos.z();
            float u = UVPair.unpackU(quad.packedUV(i));
            float v = UVPair.unpackV(quad.packedUV(i));
            Config.dbg(prefix + i + " xyz: " + x + "," + y + "," + z + " u,v: " + u + "," + v);
        }

    }
}
