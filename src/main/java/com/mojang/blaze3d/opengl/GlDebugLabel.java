package com.mojang.blaze3d.opengl;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.StringUtil;
import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

public abstract class GlDebugLabel {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void applyLabel(GlBuffer p_392931_) {
    }

    public void applyLabel(GlTexture p_392658_) {
    }

    public void applyLabel(GlShaderModule p_394260_) {
    }

    public void applyLabel(GlProgram p_394477_) {
    }

    public void applyLabel(VertexArrayCache.VertexArray p_391224_) {
    }

    public void pushDebugGroup(Supplier<String> p_409626_) {
    }

    public void popDebugGroup() {
    }

    public void applyLabelBuffer(int id, String labelIn) {
    }

    public void applyLabelTexture(int id, String labelIn) {
    }

    public void applyLabelShader(int id, String labelIn) {
    }

    public void applyLabelProgram(int id, String labelIn) {
    }

    public void applyLabelVertexArray(int id, String labelIn) {
    }

    public void applyLabelFramebuffer(int id, String labelIn) {
    }

    public static GlDebugLabel create(GLCapabilities p_396084_, boolean p_394830_, Set<String> p_393423_) {
        if (p_394830_) {
            if (p_396084_.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
                p_393423_.add("GL_KHR_debug");
                return new GlDebugLabel.Core();
            }

            if (p_396084_.GL_EXT_debug_label && GlDevice.USE_GL_EXT_debug_label) {
                p_393423_.add("GL_EXT_debug_label");
                return new GlDebugLabel.Ext();
            }

            LOGGER.warn("Debug labels unavailable: neither KHR_debug nor EXT_debug_label are supported");
        }

        return new GlDebugLabel.Empty();
    }

    public boolean exists() {
        return false;
    }

    static class Core extends GlDebugLabel {
        private final int maxLabelLength = GL11.glGetInteger(33512);

        @Override
        public void applyLabel(GlBuffer p_395944_) {
            Supplier<String> supplier = p_395944_.label;
            if (supplier != null) {
                String s = "Buf: " + supplier.get();
                KHRDebug.glObjectLabel(33504, p_395944_.handle, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
            }
        }

        @Override
        public void applyLabel(GlTexture p_397392_) {
            String s = "Tex: " + p_397392_.getLabel();
            KHRDebug.glObjectLabel(5890, p_397392_.id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(GlShaderModule p_397238_) {
            String s = "Sh: " + p_397238_.getDebugLabel();
            KHRDebug.glObjectLabel(33505, p_397238_.getShaderId(), StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(GlProgram p_394299_) {
            String s = "Prog: " + p_394299_.getDebugLabel();
            KHRDebug.glObjectLabel(33506, p_394299_.getProgramId(), StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabel(VertexArrayCache.VertexArray p_392345_) {
            String s = "VA: " + p_392345_.format.toString();
            KHRDebug.glObjectLabel(32884, p_392345_.id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void pushDebugGroup(Supplier<String> p_409965_) {
            KHRDebug.glPushDebugGroup(33354, 0, p_409965_.get());
        }

        @Override
        public void popDebugGroup() {
            KHRDebug.glPopDebugGroup();
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public void applyLabelBuffer(int id, String labelIn) {
            String s = "Buf: " + labelIn;
            KHRDebug.glObjectLabel(33504, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabelTexture(int id, String labelIn) {
            String s = "Tex: " + labelIn;
            KHRDebug.glObjectLabel(5890, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabelShader(int id, String labelIn) {
            String s = "Sh: " + labelIn;
            KHRDebug.glObjectLabel(33505, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabelProgram(int id, String labelIn) {
            String s = "Prog: " + labelIn;
            KHRDebug.glObjectLabel(33506, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabelVertexArray(int id, String labelIn) {
            String s = "VA: " + labelIn;
            KHRDebug.glObjectLabel(32884, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }

        @Override
        public void applyLabelFramebuffer(int id, String labelIn) {
            String s = "FB: " + labelIn;
            KHRDebug.glObjectLabel(36160, id, StringUtil.truncateStringIfNecessary(s, this.maxLabelLength, true));
        }
    }

    static class Empty extends GlDebugLabel {
    }

    static class Ext extends GlDebugLabel {
        @Override
        public void applyLabel(GlBuffer p_391884_) {
            Supplier<String> supplier = p_391884_.label;
            if (supplier != null) {
                String s = "Buf: " + supplier.get();
                EXTDebugLabel.glLabelObjectEXT(37201, p_391884_.handle, StringUtil.truncateStringIfNecessary(s, 256, true));
            }
        }

        @Override
        public void applyLabel(GlTexture p_397714_) {
            String s = "Tex: " + p_397714_.getLabel();
            EXTDebugLabel.glLabelObjectEXT(5890, p_397714_.id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabel(GlShaderModule p_392069_) {
            String s = "Sh: " + p_392069_.getDebugLabel();
            EXTDebugLabel.glLabelObjectEXT(35656, p_392069_.getShaderId(), StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabel(GlProgram p_394908_) {
            String s = "Prog: " + p_394908_.getDebugLabel();
            EXTDebugLabel.glLabelObjectEXT(35648, p_394908_.getProgramId(), StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabel(VertexArrayCache.VertexArray p_396080_) {
            String s = "VA: " + p_396080_.format.toString();
            EXTDebugLabel.glLabelObjectEXT(32884, p_396080_.id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public void applyLabelBuffer(int id, String labelIn) {
            String s = "Buf: " + labelIn;
            EXTDebugLabel.glLabelObjectEXT(37201, id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabelTexture(int id, String labelIn) {
            String s = "Tex: " + labelIn;
            EXTDebugLabel.glLabelObjectEXT(5890, id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabelProgram(int id, String labelIn) {
            String s = "Prog: " + labelIn;
            EXTDebugLabel.glLabelObjectEXT(35648, id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabelVertexArray(int id, String labelIn) {
            String s = "VA: " + labelIn;
            EXTDebugLabel.glLabelObjectEXT(32884, id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }

        @Override
        public void applyLabelFramebuffer(int id, String labelIn) {
            String s = "FB: " + labelIn;
            EXTDebugLabel.glLabelObjectEXT(36160, id, StringUtil.truncateStringIfNecessary(s, 256, true));
        }
    }
}
