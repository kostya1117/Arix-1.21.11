package ru.arixcompany.module;

import lombok.Getter;
import ru.arixcompany.Arix;
import ru.arixcompany.event.EventHandler;
import ru.arixcompany.event.EventRepo;
import ru.arixcompany.event.player.EventKey;
import ru.arixcompany.module.modules.player.TestModule;

import java.util.ArrayList;

@Getter
public class ModuleRepo {
   public ArrayList<Module> modules = new ArrayList<>();
   public ModuleRepo() {
       modules.add(new TestModule());
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
