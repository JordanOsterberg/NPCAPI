package dev.jcsoftware.npcs.example.gui;

import dev.jcsoftware.minecraft.gui.GUI;
import dev.jcsoftware.npcs.example.NPCPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ServerMenuGUI extends GUI<NPCPlugin> {
  public ServerMenuGUI(NPCPlugin plugin) {
    super(plugin);

    set(11, new ItemStack(Material.GOLDEN_APPLE), (player, item) -> {
      player.sendMessage("<Technoblade> Looks good to me.");
      return ButtonAction.CLOSE_GUI;
    });

    set(13, new ItemStack(Material.CARROT), (player, item) -> {
      player.sendMessage("<Technoblade> You animal...");
      return ButtonAction.CLOSE_GUI;
    });

    set(15, new ItemStack(Material.REDSTONE), (player, item) -> {
      player.sendMessage("<Technoblade> Blood for the blood god!");
      return ButtonAction.CLOSE_GUI;
    });
  }

  @Override
  public int getSize() {
    return 27;
  }

  @Override
  public String getTitle() {
    return "Server Menu";
  }

  @Override
  public boolean canClose(Player player) {
    return true;
  }
}
