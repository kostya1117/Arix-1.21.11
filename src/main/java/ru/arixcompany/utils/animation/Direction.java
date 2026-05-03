package ru.arixcompany.utils.animation;


public enum Direction {
   FORWARDS,
   BACKWARDS;

   public Direction opposite() {
      return this == FORWARDS ? BACKWARDS : FORWARDS;
   }
}
