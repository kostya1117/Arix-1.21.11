package ru.arixcompany.features.module;

import lombok.Getter;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.modules.combat.Aura;
import ru.arixcompany.features.module.modules.combat.NoFriendDMG;
import ru.arixcompany.features.module.modules.misc.AutoAccept;
import ru.arixcompany.features.module.modules.misc.CameraTweaks;
import ru.arixcompany.features.module.modules.player.TestModule;
import ru.arixcompany.features.module.modules.render.HandView;
import ru.arixcompany.features.module.modules.render.Nametags;

import java.util.ArrayList;

@Getter
public class ModuleRepo {
   public ArrayList<Module> modules = new ArrayList<>();
   public ModuleRepo() {
       modules.add(new TestModule());
       modules.add(new CameraTweaks());
       modules.add(new Nametags());
       modules.add(new HandView());
       modules.add(new Aura());
       modules.add(new NoFriendDMG());
       modules.add(new AutoAccept());
   }

   public void init() {
      EventRepo.register(this);
   }

   @EventHandler
   public void onKeyInput(EventKey event) {
      if (event.getAction() == 1) {
         if (Arix.getInstance().getModuleRepo() == null) {
            return;
         }

         Module[] modules = Arix.getInstance().getModuleRepo().getBind(event.getKey());
         if (modules != null) {
            for (Module module : modules) {
               module.toggle();
            }
         }
      }
   }

   public Module getModule(Class<?> class1) {
      for (Module module1 : this.modules) {
         if (module1.getClass() == class1) {
            return module1;
         }
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
