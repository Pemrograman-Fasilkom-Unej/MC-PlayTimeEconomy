package com.darkun7.timerald.listener;

import com.darkun7.timerald.Timerald;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ChatListener implements Listener {
    private final Timerald plugin;

    public ChatListener(Timerald plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }

        // Paper's ItemStack has displayName() and asHoverEvent()
        Component itemComponent = Component.text("")
                .append(hand.displayName().colorIfAbsent(NamedTextColor.AQUA))
                .append(Component.text(""))
                .color(NamedTextColor.AQUA)
                .hoverEvent(hand.asHoverEvent());

        TextReplacementConfig config = TextReplacementConfig.builder()
                .matchLiteral("@hand")
                .replacement(itemComponent)
                .build();

        event.message(event.message().replaceText(config));

        // Inject Party Prefix if Party plugin is active
        final String prefix = getPartyPrefixSafe(player);
        if (prefix != null) {
            io.papermc.paper.chat.ChatRenderer oldRenderer = event.renderer();
            event.renderer(io.papermc.paper.chat.ChatRenderer.viewerUnaware(
                    (source, sourceDisplayName, msg) -> Component.text("[" + prefix + "] ").color(NamedTextColor.BLUE)
                            .append(sourceDisplayName.color(NamedTextColor.WHITE))
                            .append(Component.text("> ").color(NamedTextColor.WHITE))
                            .append(msg.color(NamedTextColor.WHITE))));
        }
    }

    private String getPartyPrefixSafe(Player player) {
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("Party") != null) {
            try {
                Class<?> apiClass = Class.forName("com.darkun7.party.api.PartyAPI");
                java.lang.reflect.Method method = apiClass.getMethod("getPartyPrefix", java.util.UUID.class);
                return (String) method.invoke(null, player.getUniqueId());
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
