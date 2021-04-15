package dev.jcsoftware.npcs;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NMSHelper {
  @Getter private static final NMSHelper instance = new NMSHelper();

  @Getter private final ServerVersion serverVersion;
  private final String serverVersionString;

  private NMSHelper() {
    serverVersionString = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    ServerVersion version = ServerVersion.UNKNOWN;
    for (ServerVersion option : ServerVersion.values()) {
      if (option.name().equalsIgnoreCase(serverVersionString)) {
        version = option;
      }
    }
    this.serverVersion = version;
  }

  @SneakyThrows
  public void sendPacket(Player player, Object packet) {
    if (player == null) return;
    Object handle = getHandle(player);
    Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

    playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
  }

  @SneakyThrows
  public Object getHandle(Player player) {
    return player.getClass().getMethod("getHandle").invoke(player);
  }

  @SneakyThrows
  public Class<?> getNMSClass(String name) {
    return Class.forName("net.minecraft.server." + getServerVersion() + "." + name);
  }

  @SneakyThrows
  public Class<?> getCraftBukkitClass(String name) {
    return Class.forName("org.bukkit.craftbukkit." + getServerVersion() + "." + name);
  }

  public String getServerVersionString() {
    return serverVersionString;
  }
}
