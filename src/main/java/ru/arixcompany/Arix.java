package ru.arixcompany;

import de.florianmichael.viamcp.ViaMCP;
import de.maxhenkel.voicechat.integration.ViaVersionCompatibility;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import net.conczin.immersive_optimization.client.OptimizationDebugEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import ru.arixcompany.features.command.CommandRepo;
import ru.arixcompany.features.module.modules.misc.Core;
import ru.arixcompany.features.repos.OtherRepo;
import ru.arixcompany.features.repos.ScriptRepo;
import ru.arixcompany.ui.draggable.DraggableRepo;
import ru.arixcompany.features.module.modules.combat.aura.rotation.ComponentRepo;
import ru.arixcompany.features.repos.AltRepo;
import ru.arixcompany.ui.title.alt.SessionUtil;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.file.Directories;
import ru.arixcompany.features.file.FileController;
import ru.arixcompany.features.file.FileRepo;
import ru.arixcompany.features.file.exception.FileProcessingException;
import ru.arixcompany.features.module.ModuleRepo;
import ru.arixcompany.features.module.Theme;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.PredictUtils;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.render.shader.ShadersRepo;
import ru.arixcompany.utils.render.font.FontManager;

import java.io.File;

// Боже, Арикс храни!
// Сильный, державный,
// Работай без багов, без лагов нам!
// Работай на страх врагам,
// Код православный!
// Боже, Арикс, Арикс храни!

@Getter
public class Arix implements IMinecraft {
    @Getter
    public static Arix instance;
    public static final File gameDirectory = new File(Minecraft.getInstance().gameDirectory, "arix");
    public static final String gameDirectoryPath = new File(Minecraft.getInstance().gameDirectory, "arix").getAbsolutePath();
    public boolean initialized = false;
    @Setter
    @Getter
    private Theme currentTheme = Theme.PURPLE;

    @NonFinal
    ModuleRepo moduleRepo;
    @NonFinal
    FileController fileController;
    @NonFinal
    FileRepo fileRepo;
    @NonFinal
    CommandRepo commandRepo;
    @NonFinal
    public ComponentRepo componentRepo;
    @NonFinal
    DraggableRepo draggableRepo;
    @NonFinal
    ShadersRepo shadersRepo;
    @NonFinal
    OtherRepo otherRepo;
    @NonFinal
    ScriptRepo scriptRepo;

    public Arix(){
        instance = this;
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        ViaVersionCompatibility.register();
        FontManager.init();
        DebugScreenEntries.register(OptimizationDebugEntry.ID, new OptimizationDebugEntry());

        scriptRepo = new ScriptRepo();

        shadersRepo = new ShadersRepo();
        shadersRepo.init();
        new Timer();

        otherRepo = new OtherRepo();

        moduleRepo = new ModuleRepo();
        moduleRepo.init();

        commandRepo = new CommandRepo();
        commandRepo.setup();

        componentRepo = new ComponentRepo();
        componentRepo.init();

        draggableRepo = new DraggableRepo();
        draggableRepo.init();

        new PredictUtils();

        initFileManager();
        tryAutoLogin();

        EventRepo.register(this);
        initialized = true;

        appleskin.client.HUDOverlayHandler.init();
        appleskin.client.TooltipOverlayHandler.init();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    void onExit() {
        if (isInitialized()) {
            try {
                fileController.saveFiles();
            } catch (FileProcessingException ignored) {
            } finally {
                fileController.stopAutoSave();
            }
        }
    }

    public void initFileManager() {
        Directories.createDirectories(Directories.directory, Directories.filesDirectory,
                Directories.configDirectory);

        fileRepo = new FileRepo();
        fileRepo.setup();

        fileController = new FileController(fileRepo.getClientFiles(), Directories.filesDirectory,
                Directories.configDirectory);
        try {
            fileController.loadFiles();
        } catch (FileProcessingException ignored) {
        }
    }

    private void tryAutoLogin() {
        String last = AltRepo.getLastAlt();

        if (last == null) return;

        boolean exists = AltRepo.getAlts().stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(last));

        if (!exists) return;

        SessionUtil.setSession(last);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.getAction() != 1 || mc.screen != null) return;

        Core m = moduleRepo.getModule(Core.class);
        if (m == null) return;

        if (e.getKey() == m.bind.getKey()) {
            mc.setScreen(new Gui());
        }
    }
}
