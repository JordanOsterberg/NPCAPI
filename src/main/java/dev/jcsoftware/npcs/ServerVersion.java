package dev.jcsoftware.npcs;

public enum ServerVersion {
  v1_16_R3,
  REFLECTED,
  UNKNOWN;

  @Override
  public String toString() {
    return name();
  }
}
