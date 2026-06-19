package com.extendedclip.deluxemenus.menu;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The dynamic, holder-dependent part of a menu item.
 *
 * Holds fields that are resolved per render against a MenuHolder's
 * placeholder context. Created fresh on every render from the cached
 * {@link StaticItemData} plus the current holder. Storing this in the
 * cache would defeat placeholder updates, which is why the cache only
 * holds the static side.
 */
public final class DynamicItemData {

    private final Optional<Material> material;
    private final Optional<Integer> amount;

    private final Optional<String> displayName;
    private final List<String> lore;

    private final Optional<Integer> customModelData;
    private final Optional<Color> rgbColor;
    private final Optional<ItemRarity> rarity;
    private final Optional<Boolean> hideTooltip;
    private final Optional<Boolean> enchantmentGlintOverride;
    private final Optional<NamespacedKey> tooltipStyle;
    private final Optional<NamespacedKey> itemModel;
    private final Optional<TrimMaterial> trimMaterial;
    private final Optional<TrimPattern> trimPattern;
    private final Optional<Integer> lightLevel;
    private final short damage;

    private final Optional<String> hookArgs;
    private final Map<String, Object> nbtTagValues;

    public DynamicItemData(
            final Optional<Material> material,
            final Optional<Integer> amount,
            final Optional<String> displayName,
            final List<String> lore,
            final Optional<Integer> customModelData,
            final Optional<Color> rgbColor,
            final Optional<ItemRarity> rarity,
            final Optional<Boolean> hideTooltip,
            final Optional<Boolean> enchantmentGlintOverride,
            final Optional<NamespacedKey> tooltipStyle,
            final Optional<NamespacedKey> itemModel,
            final Optional<TrimMaterial> trimMaterial,
            final Optional<TrimPattern> trimPattern,
            final Optional<Integer> lightLevel,
            final short damage,
            final Optional<String> hookArgs,
            final Map<String, Object> nbtTagValues
    ) {
        this.material = material != null ? material : Optional.empty();
        this.amount = amount != null ? amount : Optional.empty();
        this.displayName = displayName != null ? displayName : Optional.empty();
        this.lore = lore != null ? lore : Collections.emptyList();
        this.customModelData = customModelData != null ? customModelData : Optional.empty();
        this.rgbColor = rgbColor != null ? rgbColor : Optional.empty();
        this.rarity = rarity != null ? rarity : Optional.empty();
        this.hideTooltip = hideTooltip != null ? hideTooltip : Optional.empty();
        this.enchantmentGlintOverride = enchantmentGlintOverride != null ? enchantmentGlintOverride : Optional.empty();
        this.tooltipStyle = tooltipStyle != null ? tooltipStyle : Optional.empty();
        this.itemModel = itemModel != null ? itemModel : Optional.empty();
        this.trimMaterial = trimMaterial != null ? trimMaterial : Optional.empty();
        this.trimPattern = trimPattern != null ? trimPattern : Optional.empty();
        this.lightLevel = lightLevel != null ? lightLevel : Optional.empty();
        this.damage = damage;
        this.hookArgs = hookArgs != null ? hookArgs : Optional.empty();
        this.nbtTagValues = nbtTagValues != null ? nbtTagValues : Collections.emptyMap();
    }

    public Optional<Material> getMaterial() { return material; }
    public Optional<Integer> getAmount() { return amount; }
    public Optional<String> getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public Optional<Integer> getCustomModelData() { return customModelData; }
    public Optional<Color> getRgbColor() { return rgbColor; }
    public Optional<ItemRarity> getRarity() { return rarity; }
    public Optional<Boolean> getHideTooltip() { return hideTooltip; }
    public Optional<Boolean> getEnchantmentGlintOverride() { return enchantmentGlintOverride; }
    public Optional<NamespacedKey> getTooltipStyle() { return tooltipStyle; }
    public Optional<NamespacedKey> getItemModel() { return itemModel; }
    public Optional<TrimMaterial> getTrimMaterial() { return trimMaterial; }
    public Optional<TrimPattern> getTrimPattern() { return trimPattern; }
    public Optional<Integer> getLightLevel() { return lightLevel; }
    public short getDamage() { return damage; }
    public Optional<String> getHookArgs() { return hookArgs; }
    public Map<String, Object> getNbtTagValues() { return nbtTagValues; }
}
