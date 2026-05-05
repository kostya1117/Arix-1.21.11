package ru.arixcompany.features.module.modules.combat.aura;

import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookController;
import ru.arixcompany.features.module.modules.combat.aura.rotation.RotationController;

import java.util.HashMap;

public final class ComponentRepo extends HashMap<Class<? extends IComponent>, IComponent> {

    public void init() {
        add(
                new FreeLookController(),
                new RotationController());

        this.values().forEach(EventRepo::register);
    }

    public void add(IComponent... components) {
        for (IComponent component : components) {
            this.put(component.getClass(), component);
        }
    }

    public void unregister(IComponent... components) {
        for (IComponent component : components) {
            EventRepo.unregister(component);
            this.remove(component.getClass());
        }
    }

    public <T extends IComponent> T get(final Class<T> clazz) {
        return this.values()
                .stream()
                .filter(component -> component.getClass() == clazz)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}