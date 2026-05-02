package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.optifine.SmartAnimations;
import net.optifine.render.IBufferSourceListener;
import net.optifine.render.VertexBuilderDummy;
import net.optifine.util.TextureUtils;
import org.jspecify.annotations.Nullable;

public interface MultiBufferSource {
    static MultiBufferSource.BufferSource immediate(ByteBufferBuilder p_344614_) {
        return immediateWithBuffers(Object2ObjectSortedMaps.<RenderType, ByteBufferBuilder>emptyMap(), p_344614_);
    }

    static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> p_342750_, ByteBufferBuilder p_344601_) {
        return new MultiBufferSource.BufferSource(p_344601_, p_342750_);
    }

    VertexConsumer getBuffer(RenderType p_453242_);

    default void flushRenderBuffers() {
    }

    default void flushCache() {
    }

    class BufferSource implements MultiBufferSource {
        protected final ByteBufferBuilder sharedBuffer;
        protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
        protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
        protected  RenderType lastSharedType;
        private final VertexConsumer DUMMY_BUFFER = new VertexBuilderDummy(this);
        private List<IBufferSourceListener> listeners = new ArrayList<>(4);
        private int maxCachedBuffers = 0;
        private Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> cachedBuffers = new Object2ObjectLinkedOpenHashMap<>();
        private Deque<BufferBuilder> freeBufferBuilders = new ArrayDeque<>();
        private boolean keepStartedBuffers;

        protected BufferSource(ByteBufferBuilder p_344223_, SequencedMap<RenderType, ByteBufferBuilder> p_344104_) {
            this.sharedBuffer = p_344223_;
            this.fixedBuffers = new Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder>(p_344104_);
        }

        @Override
        public VertexConsumer getBuffer(RenderType p_451002_) {
            this.addCachedBuffer(p_451002_);
            BufferBuilder bufferbuilder = this.startedBuilders.get(p_451002_);
            if (bufferbuilder != null && !p_451002_.canConsolidateConsecutiveGeometry()) {
                this.endBatch(p_451002_, bufferbuilder);
                bufferbuilder = null;
            }

            if (bufferbuilder != null) {
                return p_451002_.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder;
            }

            ByteBufferBuilder bytebufferbuilder = (ByteBufferBuilder)this.fixedBuffers.get(p_451002_);
            if (bytebufferbuilder != null) {
                bufferbuilder = new BufferBuilder(bytebufferbuilder, p_451002_.mode(), p_451002_.format(), p_451002_, null);
            } else {
                if (this.lastSharedType != null) {
                    this.endBatch(this.lastSharedType);
                }

                bufferbuilder = new BufferBuilder(this.sharedBuffer, p_451002_.mode(), p_451002_.format(), p_451002_, null);
                this.lastSharedType = p_451002_;
            }

            this.startedBuilders.put(p_451002_, bufferbuilder);
            bufferbuilder.setRenderTypeBuffer(this);
            return p_451002_.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder;
        }

        public void endLastBatch() {
            if (this.lastSharedType != null) {
                this.endBatch(this.lastSharedType);
                this.lastSharedType = null;
            }
        }

        public void endBatch() {
            if (!this.startedBuilders.isEmpty()) {
                this.endLastBatch();
                if (!this.startedBuilders.isEmpty()) {
                    for (RenderType rendertype : this.fixedBuffers.keySet()) {
                        this.endBatch(rendertype);
                        if (this.startedBuilders.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        public void endBatch(RenderType p_455992_) {
            BufferBuilder bufferbuilder = this.keepStartedBuffers ? this.startedBuilders.get(p_455992_) : this.startedBuilders.remove(p_455992_);
            if (bufferbuilder != null) {
                this.endBatch(p_455992_, bufferbuilder);
            }
        }

        private void endBatch(RenderType p_455606_, BufferBuilder p_344480_) {
            this.fireFinish(p_455606_, p_344480_);
            MeshData meshdata = p_344480_.build();
            if (meshdata != null) {
                if (p_455606_.sortOnUpload()) {
                    ByteBufferBuilder bytebufferbuilder = (ByteBufferBuilder)this.fixedBuffers.getOrDefault(p_455606_, this.sharedBuffer);
                    meshdata.sortQuads(bytebufferbuilder, RenderSystem.getProjectionType().vertexSorting());
                }

                if (p_344480_.animatedSprites != null) {
                    SmartAnimations.spritesRendered(p_344480_.animatedSprites);
                }

                p_455606_.draw(meshdata);
            }

            if (p_455606_.equals(this.lastSharedType)) {
                this.lastSharedType = null;
            }
        }

        public VertexConsumer getBuffer(Identifier textureLocation, VertexConsumer bufferIn) {
            RenderType rendertype = bufferIn.getRenderType();
            if (rendertype == null) {
                return bufferIn;
            }

            textureLocation = RenderTypes.getCustomTexture(textureLocation);
            RenderType rendertype1 = rendertype.getTextured(textureLocation);
            return this.getBuffer(rendertype1);
        }

        public RenderType getLastRenderType() {
            return this.lastSharedType;
        }

        public BufferBuilder getStartedBuffer(RenderType renderType) {
            return this.startedBuilders.get(renderType);
        }

        @Override
        public void flushRenderBuffers() {
            RenderType rendertype = this.lastSharedType;
            this.keepStartedBuffers = true;
            this.endBatch();
            this.lastSharedType = rendertype;
            this.keepStartedBuffers = false;
        }

        public void restoreRenderState(RenderType renderTypeIn, BufferBuilder bufferBuilderIn) {
            if (renderTypeIn != null && bufferBuilderIn != null) {
                if (renderTypeIn != this.lastSharedType) {
                    this.endLastBatch();
                    this.lastSharedType = renderTypeIn;
                    this.startedBuilders.put(renderTypeIn, bufferBuilderIn);
                }
            }
        }

        public void addListener(IBufferSourceListener bsl) {
            this.listeners.add(bsl);
        }

        public boolean removeListener(IBufferSourceListener bsl) {
            return this.listeners.remove(bsl);
        }

        private void fireFinish(RenderType renderTypeIn, BufferBuilder bufferIn) {
            for (int i = 0; i < this.listeners.size(); i++) {
                IBufferSourceListener ibuffersourcelistener = this.listeners.get(i);
                ibuffersourcelistener.finish(renderTypeIn, bufferIn);
            }
        }

        public VertexConsumer getDummyBuffer() {
            return this.DUMMY_BUFFER;
        }

        public void enableCache() {
        }

        @Override
        public void flushCache() {
            int i = this.maxCachedBuffers;
            this.setMaxCachedBuffers(0);
            this.setMaxCachedBuffers(i);
        }

        public void disableCache() {
            this.setMaxCachedBuffers(0);
        }

        private void setMaxCachedBuffers(int maxCachedBuffers) {
            this.maxCachedBuffers = Math.max(maxCachedBuffers, 0);
            this.trimCachedBuffers();
        }

        private void addCachedBuffer(RenderType rt) {
            if (this.maxCachedBuffers > 0) {
                this.cachedBuffers.getAndMoveToLast(rt);
                if (!this.fixedBuffers.containsKey(rt)) {
                    if (this.shouldCache(rt)) {
                        this.trimCachedBuffers();
                        BufferBuilder bufferbuilder = this.freeBufferBuilders.pollLast();
                        this.cachedBuffers.put(rt, bufferbuilder);
                    }
                }
            }
        }

        private boolean shouldCache(RenderType rt) {
            Identifier identifier = rt.getTextureLocation();
            if (identifier == null) {
                return false;
            } else if (!rt.canConsolidateConsecutiveGeometry()) {
                return false;
            } else {
                String s = identifier.getPath();
                if (s.startsWith("skins/")) {
                    return false;
                } else if (s.startsWith("capes/")) {
                    return false;
                } else if (s.startsWith("capeof/")) {
                    return false;
                } else if (s.startsWith("textures/entity/horse/")) {
                    return false;
                } else {
                    return s.startsWith("textures/entity/villager/") ? false : !s.startsWith("textures/entity/warden/");
                }
            }
        }

        private void trimCachedBuffers() {
            while (this.cachedBuffers.size() > this.maxCachedBuffers) {
                RenderType rendertype = this.cachedBuffers.firstKey();
                if (rendertype == this.lastSharedType) {
                    return;
                }

                this.removeCachedBuffer(rendertype);
            }
        }

        private void removeCachedBuffer(RenderType rt) {
        }
    }
}
