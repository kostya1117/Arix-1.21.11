/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura.rotation;

import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.SprintServerRepo;

import java.util.HashMap;

public final class ComponentRepo extends HashMap<Class<? extends Component>, Component> {

    public void init() {
        this.add(RotationManager.getInstance(), new SprintServerRepo());
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
        return this.values().stream()
            .filter(c -> c.getClass() == clazz)
            .map(clazz::cast)
            .findFirst()
            .orElse(null);
    }
}
