package net.optifine.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferUtil {
    public static String getBufferHex(BufferBuilder bb) {
        return getBufferHex(bb.getByteBuffer(), bb.getStartPosition(), bb.getDrawMode(), bb.getVertexFormat(), bb.getVertexCount());
    }

    public static String getBufferHex(MeshData md) {
        ByteBufferBuilder.Result bytebufferbuilder$result = md.getVertexResult();
        MeshData.DrawState meshdata$drawstate = md.drawState();
        return getBufferHex(
            bytebufferbuilder$result.byteBuffer(), 0, meshdata$drawstate.mode(), meshdata$drawstate.format(), meshdata$drawstate.vertexCount()
        );
    }

    public static String getBufferHex(ByteBuffer buf, int startPos, VertexFormat.Mode drawMode, VertexFormat vf, int vertexCount) {
        String s = "";
        int i = -1;
        byte b0;
        if (drawMode == VertexFormat.Mode.QUADS) {
            s = "quad";
            b0 = 4;
        } else {
            if (drawMode != VertexFormat.Mode.TRIANGLES) {
                return "Invalid draw mode: " + drawMode;
            }

            s = "triangle";
            b0 = 3;
        }

        StringBuffer stringbuffer = new StringBuffer();

        for (int j = 0; j < vertexCount; j++) {
            if (j % b0 == 0) {
                stringbuffer.append(s + " " + j / b0 + "\n");
            }

            String s1 = getVertexHex(buf, startPos, vf, j);
            stringbuffer.append(s1);
            stringbuffer.append("\n");
        }

        return stringbuffer.toString();
    }

    private static String getVertexHex(ByteBuffer buf, int startPos, VertexFormat vf, int vertex) {
        StringBuffer stringbuffer = new StringBuffer();
        int i = startPos + vertex * vf.getVertexSize();

        for (VertexFormatElement vertexformatelement : vf.getElements()) {
            if (vertexformatelement.getElementCount() > 0) {
                stringbuffer.append("(");
            }

            for (int j = 0; j < vertexformatelement.getElementCount(); j++) {
                if (j > 0) {
                    stringbuffer.append(" ");
                }

                switch (vertexformatelement.type()) {
                    case FLOAT:
                        stringbuffer.append(buf.getFloat(i));
                        break;
                    case UBYTE:
                    case BYTE:
                        stringbuffer.append(buf.get(i));
                        break;
                    case USHORT:
                    case SHORT:
                        stringbuffer.append(buf.getShort(i));
                        break;
                    case UINT:
                    case INT:
                        stringbuffer.append(buf.getShort(i));
                        break;
                    default:
                        stringbuffer.append("??");
                }

                i += vertexformatelement.type().size();
            }

            if (vertexformatelement.getElementCount() > 0) {
                stringbuffer.append(")");
            }
        }

        return stringbuffer.toString();
    }

    public static String getBufferString(IntBuffer buf) {
        if (buf == null) {
            return "null";
        }

        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.append("(pos=" + buf.position() + " lim=" + buf.limit() + " cap=" + buf.capacity() + ")");
        stringbuffer.append("[");
        int i = Math.min(buf.limit(), 1024);

        for (int j = 0; j < i; j++) {
            if (j > 0) {
                stringbuffer.append(", ");
            }

            stringbuffer.append(buf.get(j));
        }

        stringbuffer.append("]");
        return stringbuffer.toString();
    }

    public static int[] toArray(IntBuffer buf) {
        int[] aint = new int[buf.limit()];

        for (int i = 0; i < aint.length; i++) {
            aint[i] = buf.get(i);
        }

        return aint;
    }

    public static FloatBuffer createDirectFloatBuffer(int capacity) {
        return GlUtil.allocateMemory(capacity << 2).asFloatBuffer();
    }

    public static void fill(FloatBuffer buf, float val) {
        buf.clear();

        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, val);
        }

        buf.clear();
    }

    public static void copyFull(FloatBuffer src, FloatBuffer dst) {
        src.position(0);
        dst.position(0);
        dst.put(src);
        src.position(0);
        dst.position(0);
    }
}
