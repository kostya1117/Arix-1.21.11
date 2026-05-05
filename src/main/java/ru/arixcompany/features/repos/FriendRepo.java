package ru.arixcompany.features.repos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class FriendRepo {
   @Getter
   public static final List<Friend> friends = new ArrayList<>();

   public static void add(String name) {
      friends.add(new Friend(name));
   }

   public Friend getFriend(String friend) {
      return friends.stream().filter(isFriend -> isFriend.getName().equals(friend)).findFirst().get();
   }

   public static boolean isFriend(String friend) {
      return friends.stream().anyMatch(isFriend -> isFriend.getName().equals(friend));
   }
   public static boolean isFriend(Entity e) {
       return friends.stream().anyMatch(isFriend -> isFriend.getName().equals(e.getName().getString()));
   }

   public static void remove(String name) {
      friends.removeIf(friend -> friend.getName().equalsIgnoreCase(name));
   }

   public static void clear() {
      friends.clear();
   }

   @AllArgsConstructor
   @Setter
   @Getter
   public static class Friend {
      private String name;
   }
}
