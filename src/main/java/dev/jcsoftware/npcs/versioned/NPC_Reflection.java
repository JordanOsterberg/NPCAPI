package dev.jcsoftware.npcs.versioned;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.jcsoftware.npcs.NMSHelper;
import dev.jcsoftware.npcs.NPC;
import dev.jcsoftware.npcs.NPCOptions;
import dev.jcsoftware.npcs.utility.StringUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class NPC_Reflection implements NPC {
  private final JavaPlugin plugin;

  private final UUID uuid = UUID.randomUUID();
  private final String name;
  private final String entityName;
  private final String texture;
  private final String signature;
  private final boolean hideNametag;

  private Object entityPlayer;

  public NPC_Reflection(JavaPlugin plugin, NPCOptions npcOptions) {
    this.plugin = plugin;

    this.name = npcOptions.getName();
    this.texture = npcOptions.getTexture();
    this.signature = npcOptions.getSignature();
    this.hideNametag = npcOptions.isHideNametag();

    if (hideNametag) {
      this.entityName = StringUtility.randomCharacters(10);
    } else {
      this.entityName = this.name;
    }

    addToWorld(npcOptions.getLocation());
  }

  private void addToWorld(Location location) {
    try {
      NMSHelper nmsHelper = NMSHelper.getInstance();

      Object minecraftServer = nmsHelper.getCraftBukkitClass("CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
      Object worldServer = nmsHelper.getCraftBukkitClass("CraftWorld").getMethod("getHandle").invoke(location.getWorld());

      GameProfile gameProfile = makeGameProfile();

      Constructor<?> entityPlayerConstructor = nmsHelper.getNMSClass("EntityPlayer").getDeclaredConstructors()[0];
      Constructor<?> interactManagerConstructor = nmsHelper.getNMSClass("PlayerInteractManager").getDeclaredConstructors()[0];
      Object interactManager = interactManagerConstructor.newInstance(worldServer);

      this.entityPlayer = entityPlayerConstructor.newInstance(minecraftServer, worldServer, gameProfile, interactManager);

      this.entityPlayer.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class).invoke(entityPlayer,
          location.getX(),
          location.getY(),
          location.getZ(),
          location.getYaw(),
          location.getPitch());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private GameProfile makeGameProfile() {
    GameProfile gameProfile = new GameProfile(uuid, entityName);
    gameProfile.getProperties().put(
        "textures",
        new Property("textures",
            texture,
            signature
        )
    );
    return gameProfile;
  }

  @Override
  public String getName() {
    return name;
  }

  private final Set<UUID> viewers = new HashSet<>();

  @Override
  public void showTo(Player player) {
    try {
      NMSHelper nmsHelper = NMSHelper.getInstance();

      viewers.add(player.getUniqueId());

      // PacketPlayOutPlayerInfo
      Object addPlayerEnum = nmsHelper.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getField("ADD_PLAYER").get(null);
      Constructor<?> packetPlayOutPlayerInfoConstructor = nmsHelper.getNMSClass("PacketPlayOutPlayerInfo")
          .getConstructor(
              nmsHelper.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction"),
              Class.forName("[Lnet.minecraft.server." + nmsHelper.getServerVersionString() + ".EntityPlayer;")
          );

      Object array = Array.newInstance(nmsHelper.getNMSClass("EntityPlayer"), 1);
      Array.set(array, 0, this.entityPlayer);

      Object packetPlayOutPlayerInfo = packetPlayOutPlayerInfoConstructor.newInstance(addPlayerEnum, array);
      sendPacket(player, packetPlayOutPlayerInfo);

      // PacketPlayOutNamedEntitySpawn
      Constructor<?> packetPlayOutNamedEntitySpawnConstructor = nmsHelper.getNMSClass("PacketPlayOutNamedEntitySpawn")
          .getConstructor(nmsHelper.getNMSClass("EntityHuman"));
      Object packetPlayOutNamedEntitySpawn = packetPlayOutNamedEntitySpawnConstructor.newInstance(this.entityPlayer);
      sendPacket(player, packetPlayOutNamedEntitySpawn);

      // Scoreboard Team
      Object scoreboardManager = Bukkit.getServer().getClass().getMethod("getScoreboardManager")
          .invoke(Bukkit.getServer());
      Object mainScoreboard = scoreboardManager.getClass().getMethod("getMainScoreboard")
          .invoke(scoreboardManager);
      Object scoreboard = mainScoreboard.getClass().getMethod("getHandle").invoke(mainScoreboard);

      Method getTeamMethod = scoreboard.getClass().getMethod("getTeam", String.class);
      Constructor<?> scoreboardTeamConstructor = nmsHelper.getNMSClass("ScoreboardTeam").getDeclaredConstructor(nmsHelper.getNMSClass("Scoreboard"), String.class);

      Object scoreboardTeam = getTeamMethod.invoke(scoreboard, entityName) == null ?
          scoreboardTeamConstructor.newInstance(scoreboard, entityName) :
          getTeamMethod.invoke(scoreboard, entityName);

      Class<?> nameTagStatusEnum = nmsHelper.getNMSClass("ScoreboardTeamBase$EnumNameTagVisibility");
      Method setNameTagVisibility = scoreboardTeam.getClass().getMethod("setNameTagVisibility", nameTagStatusEnum);

      if (hideNametag) {
        setNameTagVisibility.invoke(scoreboardTeam, nameTagStatusEnum.getField("NEVER").get(null));
      } else {
        setNameTagVisibility.invoke(scoreboardTeam, nameTagStatusEnum.getField("ALWAYS").get(null));
      }

      Class<?> collisionStatusEnum = nmsHelper.getNMSClass("ScoreboardTeamBase$EnumTeamPush");
      Method setCollisionRule = scoreboardTeam.getClass().getMethod("setCollisionRule", collisionStatusEnum);

      setCollisionRule.invoke(scoreboardTeam, collisionStatusEnum.getField("NEVER").get(null));

      if (hideNametag) {
        Object grayChatFormat = nmsHelper.getNMSClass("EnumChatFormat").getField("GRAY").get(null);
        scoreboardTeam.getClass().getMethod("setColor", nmsHelper.getNMSClass("EnumChatFormat"))
            .invoke(scoreboardTeam, grayChatFormat);

        Constructor<?> chatMessageConstructor = nmsHelper.getNMSClass("ChatMessage").getDeclaredConstructor(String.class);
        scoreboardTeam.getClass().getMethod("setPrefix", nmsHelper.getNMSClass("IChatBaseComponent"))
            .invoke(scoreboardTeam, chatMessageConstructor.newInstance(ChatColor.COLOR_CHAR + "7[NPC] "));
      }

      Class<?> packetPlayOutScoreboardTeamClass = nmsHelper.getNMSClass("PacketPlayOutScoreboardTeam");
      Constructor<?> packetPlayOutScoreboardTeamTeamIntConstructor = packetPlayOutScoreboardTeamClass.getConstructor(nmsHelper.getNMSClass("ScoreboardTeam"), int.class);
      Constructor<?> packetPlayOutScoreboardTeamTeamCollectionIntConstructor = packetPlayOutScoreboardTeamClass.getConstructor(nmsHelper.getNMSClass("ScoreboardTeam"), Collection.class, int.class);

      sendPacket(player, packetPlayOutScoreboardTeamTeamIntConstructor.newInstance(scoreboardTeam, 1));
      sendPacket(player, packetPlayOutScoreboardTeamTeamIntConstructor.newInstance(scoreboardTeam, 0));
      sendPacket(
          player,
          packetPlayOutScoreboardTeamTeamCollectionIntConstructor.newInstance(
              scoreboardTeam,
              Collections.singletonList(entityName),
              3
          )
      );

      sendHeadRotationPacket(player);

      Bukkit.getServer().getScheduler().runTaskTimer(plugin, task -> {
        Player currentlyOnline = Bukkit.getPlayer(player.getUniqueId());
        if (currentlyOnline == null || !currentlyOnline.isOnline()) {
          task.cancel();
          return;
        }

        sendHeadRotationPacket(player);
      }, 0, 2);

      Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
        try {
          Object removePlayerEnum = nmsHelper.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getField("REMOVE_PLAYER").get(null);
          Object removeFromTabPacket = packetPlayOutPlayerInfoConstructor.newInstance(removePlayerEnum, array);
          sendPacket(player, removeFromTabPacket);
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }, 20);

      Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
        fixSkinHelmetLayerForPlayer(player);
      }, 8);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void hideFrom(Player player) {
    if (!viewers.contains(player.getUniqueId())) return;
    viewers.remove(player.getUniqueId());

    try {
      Constructor<?> destroyPacketConstructor = NMSHelper.getInstance()
          .getNMSClass("PacketPlayOutEntityDestroy").getConstructor(int[].class);
      Object packet = destroyPacketConstructor.newInstance((Object) new int[]{getId()});
      sendPacket(player, packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void delete() {
    Set<Player> onlineViewers = viewers.stream()
        .map(Bukkit::getPlayer)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    onlineViewers.forEach(this::hideFrom);
  }

  private void sendHeadRotationPacket(Player player) {
    NMSHelper nmsHelper = NMSHelper.getInstance();

    Location original = getLocation();
    Location location = original.clone().setDirection(player.getLocation().subtract(original.clone()).toVector());

    byte yaw = (byte) (location.getYaw() * 256 / 360);
    byte pitch = (byte) (location.getPitch() * 256 / 360);

    try {
      // PacketPlayOutEntityHeadRotation
      Constructor<?> packetPlayOutEntityHeadRotationConstructor = nmsHelper.getNMSClass("PacketPlayOutEntityHeadRotation")
          .getConstructor(nmsHelper.getNMSClass("Entity"), byte.class);

      Object packetPlayOutEntityHeadRotation = packetPlayOutEntityHeadRotationConstructor.newInstance(this.entityPlayer, yaw);
      sendPacket(player, packetPlayOutEntityHeadRotation);

      Constructor<?> packetPlayOutEntityLookConstructor = nmsHelper.getNMSClass("PacketPlayOutEntity$PacketPlayOutEntityLook")
          .getConstructor(int.class, byte.class, byte.class, boolean.class);
      Object packetPlayOutEntityLook = packetPlayOutEntityLookConstructor.newInstance(getId(), yaw, pitch, false);
      sendPacket(player, packetPlayOutEntityLook);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void fixSkinHelmetLayerForPlayer(Player player) {
    Byte skinFixByte = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;
    sendMetadata(player, 16, skinFixByte);
  }

  private void sendMetadata(Player player, int index, Byte o) {
    NMSHelper nmsHelper = NMSHelper.getInstance();

    try {
      Object dataWatcher = entityPlayer.getClass().getMethod("getDataWatcher").invoke(entityPlayer);
      Class<?> dataWatcherRegistryClass = nmsHelper.getNMSClass("DataWatcherRegistry");
      Object registry = dataWatcherRegistryClass.getField("a").get(null);
      Method watcherCreateMethod = registry.getClass().getMethod("a", int.class);

      Method dataWatcherSetMethod = dataWatcher.getClass().getMethod("set", nmsHelper.getNMSClass("DataWatcherObject"), Object.class);
      dataWatcherSetMethod.invoke(dataWatcher, watcherCreateMethod.invoke(registry, index), o);

      Constructor<?> packetPlayOutEntityMetadataConstructor = nmsHelper.getNMSClass("PacketPlayOutEntityMetadata")
          .getDeclaredConstructor(int.class, nmsHelper.getNMSClass("DataWatcher"), boolean.class);
      sendPacket(player, packetPlayOutEntityMetadataConstructor.newInstance(getId(), dataWatcher, false));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public Location getLocation() {
    try {
      Class<?> EntityPlayer = entityPlayer.getClass();

      Object minecraftWorld = EntityPlayer.getMethod("getWorld").invoke(entityPlayer);
      Object craftWorld = minecraftWorld.getClass().getMethod("getWorld").invoke(minecraftWorld);

      double locX = (double) EntityPlayer.getMethod("locX").invoke(entityPlayer);
      double locY = (double) EntityPlayer.getMethod("locY").invoke(entityPlayer);
      double locZ = (double) EntityPlayer.getMethod("locZ").invoke(entityPlayer);
      float yaw = (float) EntityPlayer.getField("yaw").get(entityPlayer);
      float pitch = (float) EntityPlayer.getField("pitch").get(entityPlayer);

      return new Location(
          (World) craftWorld,
          locX,
          locY,
          locZ,
          yaw,
          pitch
      );
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public int getId() {
    if (entityPlayer == null) return -1;

    try {
      Method getId = entityPlayer.getClass().getSuperclass().getSuperclass().getSuperclass().getDeclaredMethod("getId");
      return (int) getId.invoke(entityPlayer);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  private void sendPacket(Player player, Object packet) {
    NMSHelper.getInstance().sendPacket(player, packet);
  }
}
