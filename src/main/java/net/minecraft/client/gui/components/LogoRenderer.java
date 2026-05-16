package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class LogoRenderer {
    public static final Identifier MINECRAFT_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minecraft.png");
    public static final Identifier EASTER_EGG_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minceraft.png");
    public static final Identifier MINECRAFT_EDITION = Identifier.withDefaultNamespace("textures/gui/title/edition.png");
    private final boolean showEasterEgg = RandomSource.create().nextFloat() < 1.0E-4;
    private final boolean keepLogoThroughFade;

    public LogoRenderer(boolean p_265300_) {
        this.keepLogoThroughFade = p_265300_;
    }

    public void renderLogo(GuiGraphics p_282217_, int p_283270_, float p_282051_) {
        this.renderLogo(p_282217_, p_283270_, p_282051_, 30);
    }

    public void renderLogo(GuiGraphics p_281856_, int p_281512_, float p_281290_, int p_282296_) {
        int i = p_281512_ / 2 - 128;
        float f = this.keepLogoThroughFade ? 1.0F : p_281290_;
        int j = ARGB.white(f);
        p_281856_.blit(RenderPipelines.GUI_TEXTURED, this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO, i, p_282296_, 0.0F, 0.0F, 256, 44, 256, 64, j);
        int k = p_281512_ / 2 - 64;
        int l = p_282296_ + 44 - 7;
        p_281856_.blit(RenderPipelines.GUI_TEXTURED, MINECRAFT_EDITION, k, l, 0.0F, 0.0F, 128, 14, 128, 16, j);
    }

    public boolean keepLogoThroughFade() {
        return this.keepLogoThroughFade;
    }
}
