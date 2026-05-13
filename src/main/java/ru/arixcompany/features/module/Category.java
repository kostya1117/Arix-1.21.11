package ru.arixcompany.features.module;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Category {
   Combat("Combat"),
   Movement("Movement"),
   Render("Render"),
   Player("Player"),
   Misc("Misc");

    private final String name;
}
