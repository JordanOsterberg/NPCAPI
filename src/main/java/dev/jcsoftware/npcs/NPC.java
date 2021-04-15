package dev.jcsoftware.npcs;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NPC {
  String getName();
  void showTo(Player player);
  void hideFrom(Player player);
  void delete();
  Location getLocation();
  int getId();
}
