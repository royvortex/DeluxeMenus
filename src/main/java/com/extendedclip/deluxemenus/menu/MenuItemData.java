package com.extendedclip.deluxemenus.menu;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

/**
 * A wrapper around a {@link StaticItemData} (the cacheable, config-derived
 * part) and a {@link DynamicItemData} (the resolved, per-render part).
 *
 * The 27 getters are preserved for backwards compatibility with the rest of
 * the codebase: static fields delegate to the static object, dynamic fields
 * to the dynamic object, and a few (material, amount, nbtTags) merge the two
 * so callers see the same shape they always did.
 *
 * Render code can either use the wrapper getters (unchanged) or call
 * {@link #getStaticData()} / {@link #getDynamicData()} to work with the split
 * parts directly (e.g. to refresh only the dynamic side via
 * {@link MenuItem#refreshDynamic}).
 */
public class MenuItemData {

    private final StaticItemData staticData;
    private final DynamicItemData dynamicData;

    public MenuItemData(final @NotNull StaticItemData staticData,
                        final @NotNull DynamicItemData dynamicData) {
        this.staticData = staticData;
        this.dynamicData = dynamicData;
    }

    public StaticItemData getStaticData() {
        return staticData;
    }

    public DynamicItemData getDynamicData() {
        return dynamicData;
    }

    public int getSlot() { return staticData.getSlot(); }
    public int getPriority() { return staticData.getPriority(); }

    public Material getMaterial() {
        return dynamicData.getMaterial().orElseGet(staticData::getMaterial);
    }

    public int getAmount() {
        return dynamicData.getAmount().orElseGet(staticData::getAmount);
    }

    public Optional<String> getDisplayName() {
        return dynamicData.getDisplayName();
    }

    public List<String> getLore() {
        return dynamicData.getLore();
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return staticData.getEnchantments();
    }

    public short getDamage() {
        return dynamicData.getDamage();
    }

    public Optional<Integer> getCustomModelData() {
        return dynamicData.getCustomModelData();
    }

    public boolean isUnbreakable() {
        return staticData.isUnbreakable();
    }

    public List<ItemFlag> getItemFlags() {
        return staticData.getItemFlags();
    }

    public Optional<Color> getRgbColor() {
        return dynamicData.getRgbColor();
    }

    public List<PotionEffect> getPotionEffects() {
        return staticData.getPotionEffects();
    }

    public Optional<ItemRarity> getRarity() {
        return dynamicData.getRarity();
    }

    public Optional<Boolean> getHideTooltip() {
        return dynamicData.getHideTooltip();
    }

    public Optional<Boolean> getEnchantmentGlintOverride() {
        return dynamicData.getEnchantmentGlintOverride();
    }

    public Optional<NamespacedKey> getTooltipStyle() {
        return dynamicData.getTooltipStyle();
    }

    public Optional<NamespacedKey> getItemModel() {
        return dynamicData.getItemModel();
    }

    public Optional<TrimMaterial> getTrimMaterial() {
        return dynamicData.getTrimMaterial();
    }

    public Optional<TrimPattern> getTrimPattern() {
        return dynamicData.getTrimPattern();
    }

    public Optional<Integer> getLightLevel() {
        return dynamicData.getLightLevel();
    }

    public Map<String, Object> getNbtTags() {
        return dynamicData.getNbtTagValues().isEmpty() ? Collections.emptyMap() : new HashMap<>(dynamicData.getNbtTagValues());
    }

    public Optional<String> getHookName() {
        return staticData.getHookName();
    }

    public Optional<String> getHookArgs() {
        return dynamicData.getHookArgs();
    }

    public Optional<String> getBase64Data() {
        return staticData.getBase64Data();
    }

    public boolean isPlayerItem() {
        return staticData.isPlayerItem();
    }

    public boolean isWaterBottle() {
        return staticData.isWaterBottle();
    }
}
