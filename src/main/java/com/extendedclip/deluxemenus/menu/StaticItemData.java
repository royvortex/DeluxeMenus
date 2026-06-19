package com.extendedclip.deluxemenus.menu;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The static, config-derived, cacheable part of a menu item.
 *
 * Holds fields that don't depend on per-render placeholder values, plus the
 * raw templates for fields that do. The cache stores instances of this class;
 * the dynamic fields (displayName, lore, etc.) are resolved per render by
 * {@link MenuItem#refreshDynamic}.
 *
 * Field rules:
 *   - material: present iff the material has no placeholder prefix
 *   - amount:   -1 if dynamic_amount is set, else the resolved amount
 *   - hookName: present iff a hook matched the material (static per item)
 *   - base64Data: present iff the material was a base64 stack prefix
 *   - the *Template fields: present iff the option has a value (placeholder or not)
 */
public final class StaticItemData {

    private final int slot;
    private final int priority;

    private final Material material;
    private final int amount;

    private final Map<Enchantment, Integer> enchantments;
    private final List<ItemFlag> itemFlags;
    private final boolean unbreakable;
    private final List<PotionEffect> potionEffects;

    private final boolean isPlayerItem;
    private final boolean isWaterBottle;

    private final List<String> nbtStringTemplates;
    private final List<String> nbtByteTemplates;
    private final List<String> nbtShortTemplates;
    private final List<String> nbtIntTemplates;

    private final Optional<String> hookName;
    private final Optional<String> base64Data;

    private final Optional<String> materialTemplate;
    private final Optional<String> amountTemplate;
    private final Optional<String> displayNameTemplate;
    private final List<String> loreTemplates;
    private final Optional<String> customModelDataTemplate;
    private final Optional<String> rgbTemplate;
    private final Optional<String> rarityTemplate;
    private final Optional<String> hideTooltipTemplate;
    private final Optional<String> enchantmentGlintOverrideTemplate;
    private final Optional<String> tooltipStyleTemplate;
    private final Optional<String> itemModelTemplate;
    private final Optional<String> trimMaterialTemplate;
    private final Optional<String> trimPatternTemplate;
    private final Optional<String> lightLevelTemplate;
    private final Optional<String> damageTemplate;
    private final Optional<String> hookArgsTemplate;

    public StaticItemData(
            final int slot,
            final int priority,
            final Material material,
            final int amount,
            final Map<Enchantment, Integer> enchantments,
            final List<ItemFlag> itemFlags,
            final boolean unbreakable,
            final List<PotionEffect> potionEffects,
            final boolean isPlayerItem,
            final boolean isWaterBottle,
            final List<String> nbtStringTemplates,
            final List<String> nbtByteTemplates,
            final List<String> nbtShortTemplates,
            final List<String> nbtIntTemplates,
            final Optional<String> hookName,
            final Optional<String> base64Data,
            final Optional<String> materialTemplate,
            final Optional<String> amountTemplate,
            final Optional<String> displayNameTemplate,
            final List<String> loreTemplates,
            final Optional<String> customModelDataTemplate,
            final Optional<String> rgbTemplate,
            final Optional<String> rarityTemplate,
            final Optional<String> hideTooltipTemplate,
            final Optional<String> enchantmentGlintOverrideTemplate,
            final Optional<String> tooltipStyleTemplate,
            final Optional<String> itemModelTemplate,
            final Optional<String> trimMaterialTemplate,
            final Optional<String> trimPatternTemplate,
            final Optional<String> lightLevelTemplate,
            final Optional<String> damageTemplate,
            final Optional<String> hookArgsTemplate
    ) {
        this.slot = slot;
        this.priority = priority;
        this.material = material;
        this.amount = amount;
        this.enchantments = enchantments;
        this.itemFlags = itemFlags;
        this.unbreakable = unbreakable;
        this.potionEffects = potionEffects;
        this.isPlayerItem = isPlayerItem;
        this.isWaterBottle = isWaterBottle;
        this.nbtStringTemplates = nbtStringTemplates;
        this.nbtByteTemplates = nbtByteTemplates;
        this.nbtShortTemplates = nbtShortTemplates;
        this.nbtIntTemplates = nbtIntTemplates;
        this.hookName = hookName;
        this.base64Data = base64Data;
        this.materialTemplate = materialTemplate;
        this.amountTemplate = amountTemplate;
        this.displayNameTemplate = displayNameTemplate;
        this.loreTemplates = loreTemplates;
        this.customModelDataTemplate = customModelDataTemplate;
        this.rgbTemplate = rgbTemplate;
        this.rarityTemplate = rarityTemplate;
        this.hideTooltipTemplate = hideTooltipTemplate;
        this.enchantmentGlintOverrideTemplate = enchantmentGlintOverrideTemplate;
        this.tooltipStyleTemplate = tooltipStyleTemplate;
        this.itemModelTemplate = itemModelTemplate;
        this.trimMaterialTemplate = trimMaterialTemplate;
        this.trimPatternTemplate = trimPatternTemplate;
        this.lightLevelTemplate = lightLevelTemplate;
        this.damageTemplate = damageTemplate;
        this.hookArgsTemplate = hookArgsTemplate;
    }

    public int getSlot() { return slot; }
    public int getPriority() { return priority; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public List<ItemFlag> getItemFlags() { return itemFlags; }
    public boolean isUnbreakable() { return unbreakable; }
    public List<PotionEffect> getPotionEffects() { return potionEffects; }
    public boolean isPlayerItem() { return isPlayerItem; }
    public boolean isWaterBottle() { return isWaterBottle; }
    public List<String> getNbtStringTemplates() { return nbtStringTemplates; }
    public List<String> getNbtByteTemplates() { return nbtByteTemplates; }
    public List<String> getNbtShortTemplates() { return nbtShortTemplates; }
    public List<String> getNbtIntTemplates() { return nbtIntTemplates; }
    public Optional<String> getHookName() { return hookName; }
    public Optional<String> getBase64Data() { return base64Data; }
    public Optional<String> getMaterialTemplate() { return materialTemplate; }
    public Optional<String> getAmountTemplate() { return amountTemplate; }
    public Optional<String> getDisplayNameTemplate() { return displayNameTemplate; }
    public List<String> getLoreTemplates() { return loreTemplates != null ? loreTemplates : Collections.emptyList(); }
    public Optional<String> getCustomModelDataTemplate() { return customModelDataTemplate; }
    public Optional<String> getRgbTemplate() { return rgbTemplate; }
    public Optional<String> getRarityTemplate() { return rarityTemplate; }
    public Optional<String> getHideTooltipTemplate() { return hideTooltipTemplate; }
    public Optional<String> getEnchantmentGlintOverrideTemplate() { return enchantmentGlintOverrideTemplate; }
    public Optional<String> getTooltipStyleTemplate() { return tooltipStyleTemplate; }
    public Optional<String> getItemModelTemplate() { return itemModelTemplate; }
    public Optional<String> getTrimMaterialTemplate() { return trimMaterialTemplate; }
    public Optional<String> getTrimPatternTemplate() { return trimPatternTemplate; }
    public Optional<String> getLightLevelTemplate() { return lightLevelTemplate; }
    public Optional<String> getDamageTemplate() { return damageTemplate; }
    public Optional<String> getHookArgsTemplate() { return hookArgsTemplate; }

    /**
     * True iff every dynamic-relevant field has no template, meaning a render
     * can short-circuit and reuse the cached data unchanged.
     */
    public boolean isFullyStatic() {
        return !materialTemplate.isPresent()
                && !amountTemplate.isPresent()
                && !displayNameTemplate.isPresent()
                && loreTemplates.isEmpty()
                && !customModelDataTemplate.isPresent()
                && !rgbTemplate.isPresent()
                && !rarityTemplate.isPresent()
                && !hideTooltipTemplate.isPresent()
                && !enchantmentGlintOverrideTemplate.isPresent()
                && !tooltipStyleTemplate.isPresent()
                && !itemModelTemplate.isPresent()
                && !trimMaterialTemplate.isPresent()
                && !trimPatternTemplate.isPresent()
                && !lightLevelTemplate.isPresent()
                && !damageTemplate.isPresent()
                && !hookArgsTemplate.isPresent()
                && nbtStringTemplates.isEmpty()
                && nbtByteTemplates.isEmpty()
                && nbtShortTemplates.isEmpty()
                && nbtIntTemplates.isEmpty();
    }
}
