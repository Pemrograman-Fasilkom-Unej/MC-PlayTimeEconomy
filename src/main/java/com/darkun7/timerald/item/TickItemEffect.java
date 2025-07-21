package com.darkun7.timerald.item;

import org.bukkit.entity.Player;

public interface TickItemEffect {
    boolean isActive(Player player); // Check if condition is true (e.g., helmet is custom head)
    void apply(Player player);       // Apply effects
    void remove(Player player);      // Remove effects when no longer valid
}
