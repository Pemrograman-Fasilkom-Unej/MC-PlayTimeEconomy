package com.darkun7.timerald.item.items;

import com.darkun7.timerald.Timerald;
import com.darkun7.timerald.item.OnUseItem;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BookKnowledge implements OnUseItem {

    private final String displayName;
    private final Material material;
    private final Material material2;
    private final Boolean enchanted;
    private final List<String> lore;
    private final List<PotionEffect> effects;
    private final Particle particle;
    private final int particleAmount;
    private final Sound useSound;
    private final Timerald plugin;
    private final int xpToStore;

    private final NamespacedKey knowledgeKey;
    

    public BookKnowledge(Timerald plugin) {
        this.plugin = plugin;
        this.knowledgeKey = new NamespacedKey(plugin, "stored_knowledge");

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.book-knowledge");
        if (section == null) {
            throw new IllegalStateException("Missing 'shop.book-knowledge' config section.");
        }

        this.displayName = section.getString("name", "§f<§2Book §fof §aKnowledge>");
        this.material = Material.getMaterial(section.getString("material", "BOOK_AND_QUILL").toUpperCase());
        this.material2 = Material.getMaterial(section.getString("material2", "WRITTEN_BOOK").toUpperCase());
        this.lore = section.getStringList("lore");
        this.xpToStore = section.getInt("xp", 910);
        this.enchanted = section.getBoolean("enchanted", false);

        // Load effects
        this.effects = new ArrayList<>();
        for (String line : section.getStringList("effects")) {
            try {
                String[] parts = line.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                int duration = Integer.parseInt(parts[1]) * 20;
                int amplifier = Integer.parseInt(parts[2]) - 1;
                if (type != null) {
                    effects.add(new PotionEffect(type, duration, amplifier, false, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid effect format in book-knowledge config: " + line);
            }
        }

        // Particle
        String particleType = section.getString("particles.type", "ENCHANTMENT_TABLE");
        this.particle = Particle.valueOf(particleType.toUpperCase());
        this.particleAmount = section.getInt("particles.amount", 20);

        // Sound
        String soundName = section.getString("sound", "BLOCK_ENCHANTMENT_TABLE_USE");
        Sound tempSound = Sound.BLOCK_ENCHANTMENT_TABLE_USE; // Default
        try {
            tempSound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound in config: '" + soundName + "'. Using default.");
        }
        this.useSound = tempSound;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        Material type = item.getType();
        if (type != material && type != material2) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.hasLore()) return false;

        List<String> itemLore = meta.getLore();
        return itemLore.size() > 1 && lore.size() > 1 && itemLore.get(1).startsWith(lore.get(1));
    }

    @Override
    public void onUse(Player player, ItemStack item, PlayerInteractEvent event) {
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.closeInventory();
            player.updateInventory();
        }, 1L);
        if (item.getType() == material) {
            if (getTotalXP(player) >= xpToStore) {
                // Deduct XP
                int newXp = getTotalXP(player) - xpToStore;
                setTotalXP(player, newXp);

                // Create sealed written book
                ItemStack writtenBook = new ItemStack(material2);
                BookMeta bookMeta = (BookMeta) writtenBook.getItemMeta();

                bookMeta.setDisplayName(displayName);
                bookMeta.setAuthor(player.getName());

                List<String> sealedLore = new ArrayList<>(lore);
                sealedLore.add(ChatColor.GRAY + "Written by: " + ChatColor.GREEN + player.getName());
                bookMeta.setLore(sealedLore);

                bookMeta.getPersistentDataContainer().set(knowledgeKey, PersistentDataType.INTEGER, xpToStore);
                writtenBook.setItemMeta(bookMeta);

                item.setAmount(item.getAmount() - 1);
                player.getInventory().addItem(writtenBook);
                player.sendMessage("§aStored " + xpToStore + " XP in the Book of Knowledge.");
                successAction(player);
                return;
            } else {
                int currentPlayerXP = getTotalXP(player);
                player.sendMessage("§cYou don't have enough XP (need " + (xpToStore - currentPlayerXP) + ").");
                return;
            }

        } else if (item.getType() == material2) {
            if (!player.isSneaking()) {
                player.sendMessage("§7You must sneak to read the book.");
                return;
            }
            player.sendMessage("§aOpening book...");
            BookMeta meta = (BookMeta) item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(knowledgeKey, PersistentDataType.INTEGER)) {
                int storedXp = meta.getPersistentDataContainer().get(knowledgeKey, PersistentDataType.INTEGER);
                setTotalXP(player, getTotalXP(player) + storedXp);
                
                Random random = new Random();
                int chance = random.nextInt(100);
                boolean loseItem = chance < 25;

                if (loseItem) {
                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage("§eThe book crumbled into dust after releasing its energy!");
                } else {
                    ItemStack bookItem = new ItemStack(material);
                    ItemMeta itemMeta = bookItem.getItemMeta();
                    itemMeta.setDisplayName(displayName);
                    itemMeta.setLore(lore);
                    if (enchanted) {
                        itemMeta.addEnchant(Enchantment.INFINITY, 1, true);
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    bookItem.setItemMeta(itemMeta);
                    player.getInventory().addItem(bookItem);
                    player.sendMessage("§aThe book remained intact, but lose it's knowledge");
                }
                item.setAmount(item.getAmount() - 1);
                player.sendMessage("§aYou regained " + storedXp + " XP from the sealed book.");
            }
            successAction(player);
        }
    }

    private void successAction(Player player) {
        // Apply effects
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        // Visual and audio feedback
        if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation(), particleAmount, 0.5, 1, 0.5, 0.01);
        }

        if (useSound != null) {
            player.playSound(player.getLocation(), useSound, 1.0f, 1.0f);
        }
        return;
    }

    // --- XP Helpers ---

    private int getTotalXP(Player player) {
        int level = player.getLevel();
        float progress = player.getExp();
        int xp = 0;
        for (int i = 0; i < level; i++) {
            xp += getXpToLevel(i);
        }
        xp += Math.round(getXpToLevel(level) * progress);
        return xp;
    }

    private void setTotalXP(Player player, int xp) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        int level = 0;
        while (xp >= getXpToLevel(level)) {
            xp -= getXpToLevel(level);
            level++;
        }

        player.setLevel(level);
        if (getXpToLevel(level) > 0) {
            player.setExp(xp / (float) getXpToLevel(level));
        }
    }

    private int getXpToLevel(int level) {
        if (level >= 0 && level <= 15) return 2 * level + 7;
        else if (level <= 30) return 5 * level - 38;
        else return 9 * level - 158;
    }

    public static int getXpToNext(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }
}
