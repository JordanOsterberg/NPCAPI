package dev.jcsoftware.npcs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.jcsoftware.npcs.events.NPCClickAction;
import dev.jcsoftware.npcs.events.NPCInteractionEvent;
import dev.jcsoftware.npcs.versioned.NPC_Reflection;
import dev.jcsoftware.npcs.versioned.NPC_v1_16_R3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NPCManager {
  private final JavaPlugin plugin;
  private final boolean useReflection;

  private final Set<NPC> registeredNPCs = new HashSet<>();

  public NPCManager(JavaPlugin plugin, boolean useReflection) {
    this.plugin = plugin;
    this.useReflection = useReflection;

    ProtocolLibrary.getProtocolManager().addPacketListener(
        new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
          @Override
          public void onPacketReceiving(PacketEvent event) {
            EnumWrappers.EntityUseAction useAction = event.getPacket().getEntityUseActions().read(0);
            int entityId = event.getPacket().getIntegers().read(0);
            handleEntityClick(event.getPlayer(), entityId, NPCClickAction.fromProtocolLibAction(useAction));
          }
        }
    );
  }

  private final Cache<Player, NPC> clickedNPCCache = CacheBuilder.newBuilder()
      .expireAfterWrite(1L, TimeUnit.SECONDS)
      .build();

  private void handleEntityClick(Player player, int entityId, NPCClickAction action) {
    registeredNPCs.stream()
        .filter(npc -> npc.getId() == entityId)
        .forEach(npc -> Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
      NPC previouslyClickedNPC = clickedNPCCache.getIfPresent(player);
      if (previouslyClickedNPC != null && previouslyClickedNPC.equals(npc)) return; // If they've clicked this same NPC in the last 0.5 seconds ignore this click
      clickedNPCCache.put(player, npc);

      NPCInteractionEvent event = new NPCInteractionEvent(npc, player, action);
      Bukkit.getPluginManager().callEvent(event);
    }, 2));
  }

  public NPC newNPC(NPCOptions options) {
    ServerVersion serverVersion = NMSHelper.getInstance().getServerVersion();
    NPC npc = null;

    if (useReflection) {
      serverVersion = ServerVersion.REFLECTED;
    }

    switch (serverVersion) {
      case REFLECTED:
        npc = new NPC_Reflection(plugin, options);
        break;
      case v1_16_R3:
        npc = new NPC_v1_16_R3(plugin, options);
        break;
    }

    if (npc == null) {
      throw new IllegalStateException("Invalid server version " + serverVersion + ". This plugin needs to be updated!");
    }

    registeredNPCs.add(npc);
    return npc;
  }

  public Optional<NPC> findNPC(String name) {
    return registeredNPCs.stream()
        .filter(npc -> npc.getName().equalsIgnoreCase(name))
        .findFirst();
  }

  public void deleteNPC(NPC npc) {
    npc.delete();
    registeredNPCs.remove(npc);
  }

  public void deleteAllNPCs() {
    // Copy the set to prevent concurrent modification exception
    Set<NPC> npcsCopy = new HashSet<>(registeredNPCs);
    npcsCopy.forEach(this::deleteNPC);
  }
}
