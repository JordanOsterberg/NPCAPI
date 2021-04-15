package dev.jcsoftware.npcs.events;

import com.comphenix.protocol.wrappers.EnumWrappers;

public enum NPCClickAction {
  INTERACT, INTERACT_AT, ATTACK;

  public static NPCClickAction fromProtocolLibAction(EnumWrappers.EntityUseAction action) {
    switch (action) {
      case ATTACK: return ATTACK;
      case INTERACT: return INTERACT;
      case INTERACT_AT: return INTERACT_AT;
      default: return null;
    }
  }
}