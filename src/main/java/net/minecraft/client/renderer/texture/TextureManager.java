package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.ShadersTex;
import org.slf4j.Logger;

public class TextureManager implements PreparableReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier INTENTIONAL_MISSING_TEXTURE = Identifier.withDefaultNamespace("");
    private final Map<Identifier, AbstractTexture> byPath = new HashMap<>();
    private final Set<TickableTexture> tickableTextures = new HashSet<>();
    private final ResourceManager resourceManager;
    private AbstractTexture mojangLogoTexture;

    public TextureManager(ResourceManager p_118474_) {
        this.resourceManager = p_118474_;
        NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
        this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
    }

    public void registerAndLoad(Identifier p_450653_, ReloadableTexture p_376843_) {
        try {
            p_376843_.apply(this.loadContentsSafe(p_450653_, p_376843_));
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Uploading texture");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
            crashreportcategory.setDetail("Resource location", p_376843_.resourceId());
            crashreportcategory.setDetail("Texture id", p_450653_);
            throw new ReportedException(crashreport);
        }

        this.register(p_450653_, p_376843_);
    }

    private TextureContents loadContentsSafe(Identifier p_455216_, ReloadableTexture p_378623_) {
        try {
            return loadContents(this.resourceManager, p_455216_, p_378623_);
        } catch (Exception exception) {
            LOGGER.error("Failed to load texture {} into slot {}", p_378623_.resourceId(), p_455216_, exception);
            return TextureContents.createMissing();
        }
    }

    public void registerForNextReload(Identifier p_453887_) {
        this.register(p_453887_, new SimpleTexture(p_453887_));
    }

    public void register(Identifier p_461040_, AbstractTexture p_118497_) {
        if (Reflector.MinecraftForge.exists() && this.mojangLogoTexture == null && p_461040_.equals(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION)) {
            LOGGER.info("Keep logo texture for ForgeLoadingOverlay: " + p_118497_);
            this.mojangLogoTexture = p_118497_;
        }

        AbstractTexture abstracttexture = this.byPath.put(p_461040_, p_118497_);
        if (abstracttexture != p_118497_) {
            if (abstracttexture != null && abstracttexture != this.mojangLogoTexture) {
                this.safeClose(p_461040_, abstracttexture);
            }

            if (p_118497_ instanceof TickableTexture tickabletexture) {
                this.tickableTextures.add(tickabletexture);
            }
        }
    }

    private void safeClose(Identifier p_456017_, AbstractTexture p_118510_) {
        this.tickableTextures.remove(p_118510_);

        try {
            p_118510_.close();
        } catch (Exception exception) {
            LOGGER.warn("Failed to close texture {}", p_456017_, exception);
        }
    }

    public AbstractTexture getTexture(Identifier p_453290_) {
        AbstractTexture abstracttexture = this.byPath.get(p_453290_);
        if (abstracttexture != null) {
            return abstracttexture;
        }

        SimpleTexture simpletexture = new SimpleTexture(p_453290_);
        this.registerAndLoad(p_453290_, simpletexture);
        return simpletexture;
    }

    public void tick() {
        RenderSystem.getGlDevice().debugLabels().pushDebugGroup(() -> "Animations");

        for (TickableTexture tickabletexture : this.tickableTextures) {
            tickabletexture.tick();
        }

        RenderSystem.getGlDevice().debugLabels().popDebugGroup();
    }

    public void release(Identifier p_460182_) {
        AbstractTexture abstracttexture = this.byPath.remove(p_460182_);
        if (abstracttexture != null) {
            this.safeClose(p_460182_, abstracttexture);
        }
    }

    @Override
    public void close() {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState p_427249_, Executor p_118480_, PreparableReloadListener.PreparationBarrier p_118476_, Executor p_118481_
    ) {
        Config.dbg("*** Reloading textures ***");
        Iterator iterator = this.byPath.keySet().iterator();

        while (iterator.hasNext()) {
            Identifier identifier = (Identifier)iterator.next();
            String s = identifier.getPath();
            if (s.startsWith("optifine/") || EmissiveTextures.isEmissive(identifier)) {
                AbstractTexture abstracttexture = this.byPath.get(identifier);
                if (abstracttexture instanceof AbstractTexture abstracttexture1) {
                    abstracttexture1.deleteGlTexture();
                }

                iterator.remove();
            }
        }

        RandomEntities.update();
        EmissiveTextures.update();
        ResourceManager resourcemanager = p_427249_.resourceManager();
        List<TextureManager.PendingReload> list = new ArrayList<>();
        Map<Identifier, AbstractTexture> map = new HashMap<>(this.byPath);
        map.forEach((locIn, texIn) -> {
            if (Config.isShaders() && texIn instanceof DynamicTexture dynamictexture && texIn.hasGlTexture()) {
                ShadersTex.initDynamicTextureNS(dynamictexture);
            }

            if (texIn instanceof ReloadableTexture reloadabletexture) {
                list.add(scheduleLoad(resourcemanager, locIn, reloadabletexture, p_118480_));
            }
        });
        return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
            .thenCompose(p_118476_::wait)
            .thenAcceptAsync(voidIn -> {

                for (TextureManager.PendingReload texturemanager$pendingreload : list) {
                    texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
                }
            }, p_118481_);
    }

    public void dumpAllSheets(Path p_276129_) {
        try {
            Files.createDirectories(p_276129_);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create directory {}", p_276129_, ioexception);
            return;
        }

        this.byPath.forEach((locIn, texIn) -> {
            if (texIn instanceof Dumpable dumpable) {
                try {
                    dumpable.dumpContents(locIn, p_276129_);
                } catch (Exception exception) {
                    LOGGER.error("Failed to dump texture {}", locIn, exception);
                }
            }
        });
    }

    private static TextureContents loadContents(ResourceManager p_375654_, Identifier p_451339_, ReloadableTexture p_377917_) throws IOException {
        try {
            return p_377917_.loadContents(p_375654_);
        } catch (FileNotFoundException filenotfoundexception) {
            if (p_451339_ != INTENTIONAL_MISSING_TEXTURE) {
                LOGGER.warn("Missing resource {} referenced from {}", p_377917_.resourceId(), p_451339_);
            }

            return TextureContents.createMissing();
        }
    }

    private static TextureManager.PendingReload scheduleLoad(ResourceManager p_377119_, Identifier p_454798_, ReloadableTexture p_377978_, Executor p_376135_) {
        return new TextureManager.PendingReload(p_377978_, CompletableFuture.supplyAsync(() -> {
            try {
                return loadContents(p_377119_, p_454798_, p_377978_);
            } catch (IOException ioexception) {
                throw new UncheckedIOException(ioexception);
            }
        }, p_376135_));
    }

    public boolean hasTexture(Identifier loc) {
        return this.byPath.containsKey(loc);
    }

    public Collection<AbstractTexture> getTextures() {
        return this.byPath.values();
    }

    public Collection<Identifier> getTextureLocations() {
        return this.byPath.keySet();
    }

    public void bindTexture(Identifier resource) {
        AbstractTexture abstracttexture = this.getTexture(resource);
        abstracttexture.bindTexture();
    }

    record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
    }
}
