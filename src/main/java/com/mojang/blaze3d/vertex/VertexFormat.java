package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraftforge.client.extensions.IForgeVertexFormat;
import org.jspecify.annotations.Nullable;

@DontObfuscate
public class VertexFormat implements IForgeVertexFormat {
    public static final int UNKNOWN_ELEMENT = -1;
    private List<VertexFormatElement> elements;
    private List<String> names;
    private int vertexSize;
    private int elementsMask;
    private int[] offsetsByElement = new int[32];
    private @Nullable GpuBuffer immediateDrawVertexBuffer;
    private @Nullable GpuBuffer immediateDrawIndexBuffer;
    private String name;
    private int positionElementOffset = -1;
    private int normalElementOffset = -1;
    private int colorElementOffset = -1;
    private Int2IntMap uvOffsetsById = new Int2IntArrayMap();
    private ImmutableMap<String, VertexFormatElement> elementMapping;
    private boolean extended;

    VertexFormat(List<VertexFormatElement> p_343616_, List<String> p_345241_, IntList p_345522_, int p_344162_) {
        this.elements = p_343616_;
        this.names = p_345241_;
        this.vertexSize = p_344162_;
        this.elementsMask = p_343616_.stream().mapToInt(VertexFormatElement::mask).reduce(0, (p_344142_, p_345074_) -> p_344142_ | p_345074_);
        ImmutableMap.Builder<String, VertexFormatElement> builder = ImmutableMap.builder();

        for (int i = 0; i < p_343616_.size(); i++) {
            builder.put(p_345241_.get(i), p_343616_.get(i));
        }

        this.elementMapping = builder.buildOrThrow();

        for (int l = 0; l < this.offsetsByElement.length; l++) {
            VertexFormatElement vertexformatelement = VertexFormatElement.byId(l);
            int j = vertexformatelement != null ? p_343616_.indexOf(vertexformatelement) : -1;
            this.offsetsByElement[l] = j != -1 ? p_345522_.getInt(j) : -1;
            if (vertexformatelement != null) {
                VertexFormatElement.Usage vertexformatelement$usage = vertexformatelement.usage();
                int k = this.offsetsByElement[l];
                if (vertexformatelement$usage == VertexFormatElement.Usage.POSITION) {
                    this.positionElementOffset = k;
                } else if (vertexformatelement$usage == VertexFormatElement.Usage.NORMAL) {
                    this.normalElementOffset = k;
                } else if (vertexformatelement$usage == VertexFormatElement.Usage.COLOR) {
                    this.colorElementOffset = k;
                } else if (vertexformatelement$usage == VertexFormatElement.Usage.UV) {
                    this.uvOffsetsById.put(vertexformatelement.index(), k);
                }
            }
        }
    }

    public static VertexFormat.Builder builder() {
        return new VertexFormat.Builder();
    }

    @Override
    public String toString() {
        return "VertexFormat " + this.name + ", " + this.vertexSize + "B, " + this.names;
    }

    public int getVertexSize() {
        return this.vertexSize;
    }

    public List<VertexFormatElement> getElements() {
        return this.elements;
    }

    public List<String> getElementAttributeNames() {
        return this.names;
    }

    public int[] getOffsetsByElement() {
        return this.offsetsByElement;
    }

    public int getOffset(VertexFormatElement p_342517_) {
        return this.offsetsByElement[p_342517_.id()];
    }

    public boolean contains(VertexFormatElement p_345196_) {
        return (this.elementsMask & p_345196_.mask()) != 0;
    }

    public int getElementsMask() {
        return this.elementsMask;
    }

    public String getElementName(VertexFormatElement p_345336_) {
        int i = this.elements.indexOf(p_345336_);
        if (i == -1) {
            throw new IllegalArgumentException(p_345336_ + " is not contained in format");
        } else {
            return this.names.get(i);
        }
    }

    @Override
    public boolean equals(Object p_86026_) {
        return this == p_86026_
            ? true
            : p_86026_ instanceof VertexFormat vertexformat
                && this.elementsMask == vertexformat.elementsMask
                && this.vertexSize == vertexformat.vertexSize
                && this.names.equals(vertexformat.names)
                && Arrays.equals(this.offsetsByElement, vertexformat.offsetsByElement);
    }

    @Override
    public int hashCode() {
        return this.elementsMask * 31 + Arrays.hashCode(this.offsetsByElement);
    }

    private static GpuBuffer uploadToBuffer(@Nullable GpuBuffer p_410777_, ByteBuffer p_410807_, @GpuBuffer.Usage int p_410790_, Supplier<String> p_410792_) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if (GraphicsWorkarounds.get(gpudevice).alwaysCreateFreshImmediateBuffer()) {
            if (p_410777_ != null) {
                p_410777_.close();
            }

            return gpudevice.createBuffer(p_410792_, p_410790_, p_410807_);
        } else {
            if (p_410777_ == null) {
                p_410777_ = gpudevice.createBuffer(p_410792_, p_410790_, p_410807_);
            } else {
                CommandEncoder commandencoder = gpudevice.createCommandEncoder();
                if (p_410777_.size() < p_410807_.remaining()) {
                    p_410777_.close();
                    p_410777_ = gpudevice.createBuffer(p_410792_, p_410790_, p_410807_);
                } else {
                    commandencoder.writeToBuffer(p_410777_.slice(), p_410807_);
                }
            }

            return p_410777_;
        }
    }

    public GpuBuffer uploadImmediateVertexBuffer(ByteBuffer p_394343_) {
        this.immediateDrawVertexBuffer = uploadToBuffer(this.immediateDrawVertexBuffer, p_394343_, 40, () -> "Immediate vertex buffer for " + this);
        return this.immediateDrawVertexBuffer;
    }

    public GpuBuffer uploadImmediateIndexBuffer(ByteBuffer p_391835_) {
        this.immediateDrawIndexBuffer = uploadToBuffer(this.immediateDrawIndexBuffer, p_391835_, 72, () -> "Immediate index buffer for " + this);
        return this.immediateDrawIndexBuffer;
    }

    public int getOffset(int index) {
        return this.offsetsByElement[index];
    }

    public boolean hasPosition() {
        return this.positionElementOffset >= 0;
    }

    public int getPositionOffset() {
        return this.positionElementOffset;
    }

    public boolean hasNormal() {
        return this.normalElementOffset >= 0;
    }

    public int getNormalOffset() {
        return this.normalElementOffset;
    }

    public boolean hasColor() {
        return this.colorElementOffset >= 0;
    }

    public int getColorOffset() {
        return this.colorElementOffset;
    }

    public boolean hasUV(int id) {
        return this.uvOffsetsById.containsKey(id);
    }

    public int getUvOffsetById(int id) {
        return this.uvOffsetsById.get(id);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void copyFrom(VertexFormat vf) {
        this.elements = vf.elements;
        this.names = vf.names;
        this.vertexSize = vf.vertexSize;
        this.elementsMask = vf.elementsMask;
        this.offsetsByElement = vf.offsetsByElement;
        this.immediateDrawVertexBuffer = vf.immediateDrawVertexBuffer;
        this.name = vf.name;
        this.positionElementOffset = vf.positionElementOffset;
        this.normalElementOffset = vf.normalElementOffset;
        this.colorElementOffset = vf.colorElementOffset;
        this.uvOffsetsById = vf.uvOffsetsById;
        this.elementMapping = vf.elementMapping;
        this.extended = vf.extended;
    }

    public VertexFormat duplicate() {
        VertexFormat.Builder vertexformat$builder = builder();
        vertexformat$builder.addAll(this);
        return vertexformat$builder.build();
    }

    public ImmutableMap<String, VertexFormatElement> getElementMapping() {
        return this.elementMapping;
    }

    public int getIntegerSize() {
        return this.getVertexSize() / 4;
    }

    public boolean isExtended() {
        return this.extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public VertexFormatElement getElement(String name) {
        return this.elementMapping.get(name);
    }

    @DontObfuscate
    public static class Builder {
        private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        private final IntList offsets = new IntArrayList();
        private int offset;

        public VertexFormat.Builder add(String p_343401_, VertexFormatElement p_345244_) {
            this.elements.put(p_343401_, p_345244_);
            this.offsets.add(this.offset);
            this.offset = this.offset + p_345244_.byteSize();
            return this;
        }

        public VertexFormat.Builder padding(int p_345477_) {
            this.offset += p_345477_;
            return this;
        }

        public VertexFormat build() {
            ImmutableMap<String, VertexFormatElement> immutablemap = this.elements.buildOrThrow();
            ImmutableList<VertexFormatElement> immutablelist = immutablemap.values().asList();
            ImmutableList<String> immutablelist1 = immutablemap.keySet().asList();
            return new VertexFormat(immutablelist, immutablelist1, this.offsets, this.offset);
        }

        public VertexFormat.Builder addAll(VertexFormat vf) {
            for (VertexFormatElement vertexformatelement : vf.getElements()) {
                String s = vf.getElementName(vertexformatelement);
                this.add(s, vertexformatelement);
            }

            while (this.offset < vf.getVertexSize()) {
                this.padding(1);
            }

            return this;
        }
    }

    public enum IndexType {
        SHORT(2),
        INT(4);

        public final int bytes;

        IndexType(final int p_166930_) {
            this.bytes = p_166930_;
        }

        public static VertexFormat.IndexType least(int p_166934_) {
            return (p_166934_ & -65536) != 0 ? INT : SHORT;
        }
    }

    public enum Mode {
        LINES(2, 2, false),
        DEBUG_LINES(2, 2, false),
        DEBUG_LINE_STRIP(2, 1, true),
        POINTS(1, 1, false),
        TRIANGLES(3, 3, false),
        TRIANGLE_STRIP(3, 1, true),
        TRIANGLE_FAN(3, 1, true),
        QUADS(4, 4, false);

        public final int primitiveLength;
        public final int primitiveStride;
        public final boolean connectedPrimitives;

        Mode(final int p_231238_, final int p_231239_, final boolean p_231241_) {
            this.primitiveLength = p_231238_;
            this.primitiveStride = p_231239_;
            this.connectedPrimitives = p_231241_;
        }

        public int indexCount(int p_166959_) {
            return switch (this) {
                case LINES, QUADS -> p_166959_ / 4 * 6;
                case DEBUG_LINES, DEBUG_LINE_STRIP, POINTS, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN -> p_166959_;
                default -> 0;
            };
        }
    }
}
