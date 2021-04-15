package dev.jcsoftware.npcs.example;

import dev.jcsoftware.minecraft.gui.GUIAPI;
import dev.jcsoftware.npcs.NPC;
import dev.jcsoftware.npcs.NPCManager;
import dev.jcsoftware.npcs.NPCOptions;
import dev.jcsoftware.npcs.events.NPCClickAction;
import dev.jcsoftware.npcs.events.NPCInteractionEvent;
import dev.jcsoftware.npcs.example.gui.ServerMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class NPCPlugin extends JavaPlugin implements Listener {

  private NPCManager npcManager;
  private GUIAPI<NPCPlugin> guiAPI;

  private final boolean USE_REFLECTION = false;

  @Override
  public void onEnable() {
    this.npcManager = new NPCManager(this, USE_REFLECTION);
    this.guiAPI = new GUIAPI<>(this);

    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void onDisable() {
    npcManager.deleteAllNPCs();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) return true;
    Player player = (Player) sender;

    if (command.getLabel().equalsIgnoreCase("spawn")) {
      NPC npc = npcManager.newNPC(NPCOptions.builder()
          .name("Technoblade")
          .hideNametag(false)
          // See https://sessionserver.mojang.com/session/minecraft/profile/b876ec32e396476ba1158438d83c67d4?unsigned=false
          // This will give Technoblade's Texture & Signature
          .texture("ewogICJ0aW1lc3RhbXAiIDogMTYxODM1Nzg1Mjc3NSwKICAicHJvZmlsZUlkIiA6ICJiODc2ZWMzMmUzOTY0NzZiYTExNTg0MzhkODNjNjdkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWNobm9ibGFkZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ODZjMDM5ZDk2OWQxODM5MTU1MjU1ZTM4ZTdiMDZhNjI2ZWE5ZjhiYWY5Y2I1NWUwYTc3MzExZWZlMThhM2UiCiAgICB9CiAgfQp9")
          .signature("VYUQfmkBsHTXWf8tRRCiws/A/iwA+VIZ8wrbp4IdcM1CnYhZP+LTrVXSSl8bc88vQPbGxdL2Ks3Ow4cmBnGWe1ezpHWRO4vcyXRvh0AOle3XGYI31x7usryY9rr/37xLTdKqh7V7Ox4Dq9qt8Bmo8QBolpXBT6HlCbPPG6cu5AlycWTsoA6X0zvfWihLXH1suIU4LPeaORX1SpppzCGo1mz/SI0HaLM5vJIhktf8mJqP0DwUQetezb+b+LtJenoFp2lE/qRcrRF739NuwMw6tniea1dn3ftAWBH8l0r3p6uDzOAjJOxGnR5YBWfOewWF3x+k2UXkKqC01pPu1S8PbQDayP0++XsXw+28wvI/5G4U2otIoEU4lucViJPjWXmn2acE5LNq8eHaAm+5pBCmJ1TNGZkDlTHekivW1kaFh2NQCY3SyizUWjcPVE6aYZK8c2bltGOcKhgzJb7hYnjdbTX0S7KMD1csCN1bUduyv9byzvJkpVNka3LavCZCIPJ1ICpLFwQemdzaqXTp2x+5lnxKCMLu0EpDikX1Hcm86pJpW4qxXcZNRyCEwlulseIvRIgyfNzjDO2F8CYf94JqQVZ/pKonuRnJGTuWzur788JfaWcfrOv0hCUt8F5Yw1BCkBsucDhPaOwvQLPLET7+aPhuermXKsiw5UasB+OGhlA=")
          .location(player.getLocation())
          .build());
      npc.showTo(player);
    }

    return true;
  }

  @EventHandler
  private void onNPCClick(NPCInteractionEvent event) {
    NPC clicked = event.getClicked();

    if (clicked.getName().equalsIgnoreCase("Technoblade")) {
      if (event.getClickAction() == NPCClickAction.ATTACK) return;
      guiAPI.openGUI(event.getPlayer(), new ServerMenuGUI(this));
    } else {
      event.getPlayer().sendMessage("<" + clicked.getName() + "> Sorry, I don't think you're looking for me.");
    }
  }
}
