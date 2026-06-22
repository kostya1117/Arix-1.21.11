package ru.arixcompany.features.module;

import lombok.Getter;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.modules.combat.*;
import ru.arixcompany.features.module.modules.misc.AutoAccept;
import ru.arixcompany.features.module.modules.misc.CameraTweaks;
import ru.arixcompany.features.module.modules.misc.Core;
import ru.arixcompany.features.module.modules.misc.ClientSounds;
import ru.arixcompany.features.module.modules.misc.Protect;
import ru.arixcompany.features.module.modules.misc.funtime.AuctionUtils;
import ru.arixcompany.features.module.modules.misc.funtime.AutoBuy;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.features.module.modules.movement.Speed;
import ru.arixcompany.features.module.modules.movement.Strafe;
import ru.arixcompany.features.module.modules.player.*;
import ru.arixcompany.features.module.modules.render.*;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.utils.IMinecraft;

import java.util.ArrayList;

@Getter
public class ModuleRepo implements IMinecraft {
   public ArrayList<Module> modules = new ArrayList<>();
   public ModuleRepo() {
       Core core = new Core();
       core.state = true;
       modules.add(core);
       
       modules.add(new TestModule());
       modules.add(new CameraTweaks());
       modules.add(new Esp());
       modules.add(new HandView());
       modules.add(new NoFriendDMG());
       modules.add(new AutoAccept());
       modules.add(new TargetESP());
       modules.add(new AutoSprint());
       modules.add(new Strafe());
       modules.add(new ClientSounds());
       modules.add(new Protect());
       modules.add(new HitAura());
       modules.add(new AutoTotem());
       modules.add(new TargetStrafe());
       modules.add(new AutoBuy());
       modules.add(new AuctionUtils());
       modules.add(new FullBright());
       modules.add(new StorageEsp());
       modules.add(new HitBubbles());
       modules.add(new CrosshairArrows());
       modules.add(new BlockHighLight());
       modules.add(new JumpCircle());
        modules.add(new ChinaHat());
       // modules.add(new ChunkAnimator());
        modules.add(new NoRender());
       modules.add(new Velocity());
       modules.add(new Interface());
       modules.add(new Animations());
       modules.add(new NoPush());
       modules.add(new PearlPrediction());
       modules.add(new AutoRespawn());
       modules.add(new ChestStealer());
       modules.add(new Particles());
       modules.add(new AutoSwap());
       modules.add(new AspectRatio());
       modules.add(new SeeInvisibles());
       modules.add(new CustomModels());
       modules.add(new Speed());
       modules.add(new Assistant());
   }

   public void init() {
      EventRepo.register(this);
   }

   @EventHandler
   public void onKeyInput(EventKey event) {
      if (Arix.getInstance().getModuleRepo() == null) return;
      if (mc.screen != null) return;

      int key = event.getKey();

      Module[] modules = Arix.getInstance().getModuleRepo().getBind(key);
      if (modules != null) {
         for (Module module : modules) {
            if (event.getAction() == 1) {
               module.onBindPress();
            } else if (event.getAction() == 0) {
               module.onBindRelease();
            }
         }
      }

      if (event.getAction() == 1) {
         for (Module module : Arix.getInstance().getModuleRepo().modules) {
            for (ru.arixcompany.features.module.setting.Setting setting : module.settings()) {
               if (setting instanceof BooleanSetting boolSetting) {
                  if (boolSetting.getKey() == key) {
                     boolSetting.setValue(!boolSetting.isValue());
                  }
               }
            }
         }
      }
   }

    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : this.modules) {
            if (!clazz.isInstance(module)) continue;
            return clazz.cast(module);
        }
        return null;
    }

   public ArrayList<Module> getModule(Category category) {
      ArrayList<Module> modules = new ArrayList<>();

      for (Module module1 : this.modules) {
         if (module1.category == category) {
            modules.add(module1);
         }
      }

      return modules;
   }

   public Module[] getBind(int bind) {
      return Arix.getInstance().getModuleRepo().modules.stream().filter(module -> module.bind == bind).toArray(Module[]::new);
   }

   public Module getModuleByName(String name) {
      return modules.stream()
              .filter(module -> module.getName().equalsIgnoreCase(name))
              .findFirst()
              .orElse(null);
   }
}
