package ru.arixcompany.utils.aurautil;

import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.utils.aurautil.freelooksystem.FreeLookSystem;
import ru.arixcompany.utils.aurautil.rotsystem.RotationSystem;

import java.util.HashMap;

public final class SystemManager extends HashMap<Class<? extends System>, System> {


    public void init() {
        add(
                new FreeLookSystem(),
                new RotationSystem());

        this.values().forEach(EventRepo::register);
    }

    public void add(System... components) {
        for (System component : components) {
            this.put(component.getClass(), component);
        }
    }

    public void unregister(System... components) {
        for (System component : components) {
            EventRepo.unregister(component);
            this.remove(component.getClass());
        }
    }

    public <T extends System> T get(final Class<T> clazz) {
        return this.values()
                .stream()
                .filter(component -> component.getClass() == clazz)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}