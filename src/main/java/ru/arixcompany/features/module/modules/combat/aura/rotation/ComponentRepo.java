package ru.arixcompany.features.module.modules.combat.aura.rotation;


import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookUtil;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;

import java.util.HashMap;

public final class ComponentRepo extends HashMap<Class<? extends Component>, Component> {
   public void init() {
      this.add(new FreeLookUtil(), new RotationRepo());
      this.values().forEach(EventRepo::register);
   }

   public void add(Component... components) {
      for (Component component : components) {
         this.put(component.getClass(), component);
      }
   }

   public void unregister(Component... components) {
      for (Component component : components) {
         EventRepo.unregister(component);
         this.remove(component.getClass());
      }
   }

   public <T extends Component> T get(Class<T> clazz) {
      return this.values().stream().filter(component -> component.getClass() == clazz).map(clazz::cast).findFirst().orElse(null);
   }
}
