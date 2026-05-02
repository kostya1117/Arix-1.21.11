package ru.arixcompany.module;

import lombok.Getter;

public enum Category {
   Combat("Combat", "a"),
   Movement("Movement", "c"),
   Render("Render", "e"),
   Player("Player", "g"),
   Misc("Misc", "h");

   @Getter
   private final String name;
   @Getter
   private final String icon;

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }
}
