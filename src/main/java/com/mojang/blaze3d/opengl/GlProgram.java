package com.mojang.blaze3d.opengl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.renderer.ShaderManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

public class GlProgram implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet("Projection", "Lighting", "Fog", "Globals");
    public static GlProgram INVALID_PROGRAM = new GlProgram(-1, "invalid");
    private final Map<String, Uniform> uniformsByName = new HashMap<>();
    private final int programId;
    private final String debugLabel;

    private GlProgram(int p_395559_, String p_391971_) {
        this.programId = p_395559_;
        this.debugLabel = p_391971_;
    }

    public static GlProgram link(GlShaderModule p_393297_, GlShaderModule p_393267_, VertexFormat p_392588_, String p_392070_) throws ShaderManager.CompilationException {
        int i = GlStateManager.glCreateProgram();
        if (i <= 0) {
            throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + i + ")");
        }

        int j = 0;

        for (String s : p_392588_.getElementAttributeNames()) {
            VertexFormatElement vertexformatelement = p_392588_.getElement(s);
            j = vertexformatelement.getAttributeIndex();
            if (j >= 0) {
                GlStateManager._glBindAttribLocation(i, j, s);
                j++;
            }
        }

        GlStateManager.glAttachShader(i, p_393297_.getShaderId());
        GlStateManager.glAttachShader(i, p_393267_.getShaderId());
        GlStateManager.glLinkProgram(i);
        int k = GlStateManager.glGetProgrami(i, 35714);
        String s1 = GlStateManager.glGetProgramInfoLog(i, 32768);
        if (k != 0 && !s1.contains("Failed for unknown reason")) {
            if (!s1.isEmpty()) {
                LOGGER.info("Info log when linking program containing VS {} and FS {}. Log output: {}", p_393297_.getId(), p_393267_.getId(), s1);
            }

            return new GlProgram(i, p_392070_);
        } else {
            throw new ShaderManager.CompilationException(
                "Error encountered when linking program containing VS " + p_393297_.getId() + " and FS " + p_393267_.getId() + ". Log output: " + s1
            );
        }
    }

    public void setupUniforms(List<RenderPipeline.UniformDescription> p_393412_, List<String> p_395673_) {
        int i = 0;
        int j = 0;

        for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : p_393412_) {
            String s = renderpipeline$uniformdescription.name();

            Object object = switch (renderpipeline$uniformdescription.type()) {
                case UNIFORM_BUFFER -> {
                    int k2 = GL31.glGetUniformBlockIndex(this.programId, s);
                    if (k2 == -1) {
                        yield null;
                    } else {
                        int l2 = i++;
                        GL31.glUniformBlockBinding(this.programId, k2, l2);
                        yield new Uniform.Ubo(l2);
                    }
                }
                case TEXEL_BUFFER -> {
                    int k = GlStateManager._glGetUniformLocation(this.programId, s);
                    if (k == -1) {
                        LOGGER.warn("{} shader program does not use utb {} defined in the pipeline. This might be a bug.", this.debugLabel, s);
                        yield null;
                    } else {
                        int l = j++;
                        Uniform.Utb uniform$utb = new Uniform.Utb(k, l, Objects.requireNonNull(renderpipeline$uniformdescription.textureFormat()));
                        int i1 = GlStateManager._getInteger(35884);
                        GlStateManager.bindTexture(35882, uniform$utb.texture());
                        RenderSystem.getGlDevice().debugLabels().applyLabelTexture(uniform$utb.texture(), "UTB " + this.getDebugLabel());
                        GlStateManager.bindTexture(35882, i1);
                        yield uniform$utb;
                    }
                }
            };
            Uniform uniform = (Uniform)object;
            if (uniform != null) {
                this.uniformsByName.put(s, uniform);
            }
        }

        for (String s1 : p_395673_) {
            int l1 = GlStateManager._glGetUniformLocation(this.programId, s1);
            if (l1 == -1) {
                LOGGER.warn("{} shader program does not use sampler {} defined in the pipeline. This might be a bug.", this.debugLabel, s1);
            } else {
                int i2 = j++;
                this.uniformsByName.put(s1, new Uniform.Sampler(l1, i2));
            }
        }

        int j1 = GlStateManager.glGetProgrami(this.programId, 35382);

        for (int k1 = 0; k1 < j1; k1++) {
            String s2 = GL31.glGetActiveUniformBlockName(this.programId, k1);
            if (!this.uniformsByName.containsKey(s2)) {
                if (!p_395673_.contains(s2) && BUILT_IN_UNIFORMS.contains(s2)) {
                    int j2 = i++;
                    GL31.glUniformBlockBinding(this.programId, k1, j2);
                    this.uniformsByName.put(s2, new Uniform.Ubo(j2));
                } else {
                    LOGGER.warn("Found unknown and unsupported uniform {} in {}", s2, this.debugLabel);
                }
            }
        }
    }

    @Override
    public void close() {
        this.uniformsByName.values().forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
    }

    public @Nullable Uniform getUniform(String p_395714_) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(p_395714_);
    }

    @VisibleForTesting
    public int getProgramId() {
        return this.programId;
    }

    @Override
    public String toString() {
        return this.debugLabel;
    }

    public String getDebugLabel() {
        return this.debugLabel;
    }

    public Map<String, Uniform> getUniforms() {
        return this.uniformsByName;
    }
}
