package com.extendedclip.deluxemenus.menu;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;

/**
 * A pure data representation of a menu item, containing all precomputed values
 * (placeholders resolved, requirements checked) but no Bukkit ItemStack objects.
 */
public class MenuItemData {
    private final int slot;
    private final int priority;
    private final Material material;
    private final int amount;
    private final Optional<String> displayName;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final short damage;
    private final Optional<Integer> customModelData;
    private final boolean unbreakable;
    private final List<ItemFlag> itemFlags;
    private final Optional<Color> rgbColor;
    private final List<PotionEffect> potionEffects;
    private final Optional<ItemRarity> rarity;
    private final Optional<Boolean> hideTooltip;
    private final Optional<Boolean> enchantmentGlintOverride;
    private final Optional<NamespacedKey> tooltipStyle;
    private final Optional<NamespacedKey> itemModel;
    private final Optional<TrimMaterial> trimMaterial;
    private final Optional<TrimPattern> trimPattern;
    private final Optional<Integer> lightLevel;
    private final Map<String, Object> nbtTags;
    private final Optional<String> hookName;
    private final Optional<String> hookArgs;
    private final Optional<String> base64Data;
    private final boolean isPlayerItem;
    private final boolean isWaterBottle;

    public MenuItemData(
            int slot,
            int priority,
            Material material,
            int amount,
            Optional<String> displayName,
            List<String> lore,
            Map<Enchantment, Integer> enchantments,
            short damage,
            Optional<Integer> customModelData,
            boolean unbreakable,
            List<ItemFlag> itemFlags,
            Optional<Color> rgbColor,
            List<PotionEffect> potionEffects,
            Optional<ItemRarity> rarity,
            Optional<Boolean> hideTooltip,
            Optional<Boolean> enchantmentGlintOverride,
            Optional<NamespacedKey> tooltipStyle,
            Optional<NamespacedKey> itemModel,
            Optional<TrimMaterial> trimMaterial,
            Optional<TrimPattern> trimPattern,
            Optional<Integer> lightLevel,
            Map<String, Object> nbtTags,
            Optional<String> hookName,
            Optional<String> hookArgs,
            Optional<String> base64Data,
            boolean isPlayerItem,
            boolean isWaterBottle
    ) {
        this.slot = slot;
        this.priority = priority;
        this.material = material;
        this.amount = amount;
        this.displayName = displayName;
        this.lore = lore;
        this.enchantments = enchantments;
        this.damage = damage;
        this.customModelData = customModelData;
        this.unbreakable = unbreakable;
        this.itemFlags = itemFlags;
        this.rgbColor = rgbColor;
        this.potionEffects = potionEffects;
        this.rarity = rarity;
        this.hideTooltip = hideTooltip;
        this.enchantmentGlintOverride = enchantmentGlintOverride;
        this.tooltipStyle = tooltipStyle;
        this.itemModel = itemModel;
        this.trimMaterial = trimMaterial;
        this.trimPattern = trimPattern;
        this.lightLevel = lightLevel;
        this.nbtTags = nbtTags;
        this.hookName = hookName;
        this.hookArgs = hookArgs;
        this.base64Data = base64Data;
        this.isPlayerItem = isPlayerItem;
        this.isWaterBottle = isWaterBottle;
    }

    public int getSlot() { return slot; }
    public int getPriority() { return priority; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public Optional<String> getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public short getDamage() { return damage; }
    public Optional<Integer> getCustomModelData() { return customModelData; }
    public boolean isUnbreakable() { return unbreakable; }
    public List<ItemFlag> getItemFlags() { return itemFlags; }
    public Optional<Color> getRgbColor() { return rgbColor; }
    public List<PotionEffect> getPotionEffects() { return potionEffects; }
    public Optional<ItemRarity> getRarity() { return rarity; }
    public Optional<Boolean> getHideTooltip() { return hideTooltip; }
    public Optional<Boolean> getEnchantmentGlintOverride() { return enchantmentGlintOverride; }
    public Optional<NamespacedKey> getTooltipStyle() { return tooltipStyle; }
    public Optional<NamespacedKey> getItemModel() { return itemModel; }
    public Optional<TrimMaterial> getTrimMaterial() { return trimMaterial; }
    public Optional<TrimPattern> getTrimPattern() { return trimPattern; }
    public Optional<Integer> getLightLevel() { return lightLevel; }
    public Map<String, Object> getNbtTags() { return nbtTags; }
    public Optional<String> getHookName() { return hookName; }
    public Optional<String> getHookArgs() { return hookArgs; }
    public Optional<String> getBase64Data() { return base64Data; }
    public boolean isPlayerItem() { return isPlayerItem; }
    public boolean isWaterBottle() { return isWaterBottle; }
}
