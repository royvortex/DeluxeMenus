package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.hooks.ItemHook;
import com.extendedclip.deluxemenus.menu.options.CustomModelDataComponent;
import com.extendedclip.deluxemenus.menu.options.HeadType;
import com.extendedclip.deluxemenus.menu.options.LoreAppendMode;
import com.extendedclip.deluxemenus.menu.options.MenuItemOptions;
import com.extendedclip.deluxemenus.nbt.NbtProvider;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.ItemUtils;
import com.extendedclip.deluxemenus.utils.StringUtils;
import com.extendedclip.deluxemenus.utils.VersionHelper;
import com.google.common.collect.ImmutableMultimap;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.extendedclip.deluxemenus.utils.Constants.INVENTORY_ITEM_ACCESSORS;
import static com.extendedclip.deluxemenus.utils.Constants.PLACEHOLDER_PREFIX;
import static com.extendedclip.deluxemenus.utils.Constants.STACK_PREFIX;

public class MenuItem {

    private final DeluxeMenus plugin;
    private final MenuItemOptions options;
    private final String configKey;

    public MenuItem(@NotNull final DeluxeMenus plugin, @NotNull final String configKey, @NotNull final MenuItemOptions options) {
        this.plugin = plugin;
        this.configKey = configKey;
        this.options = options;
    }

    public static ItemStack base64ToItemStack(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            dataInput.close();
            Object object = dataInput.readObject();
            if (object instanceof ItemStack) {
                return (ItemStack) object;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Compute (or fetch from cache) a {@link MenuItemData} for the given holder.
     *
     * The static side is cached; the dynamic side is always resolved fresh
     * against the current holder, so placeholders reflect the latest state on
     * every call. This is what fixes the prior bug where
     * {@code updatePlaceholders: true} items silently kept stale values when
     * caching was on.
     */
    public MenuItemData computeData(@NotNull final MenuHolder holder) {
        final StaticItemData staticData = getOrComputeStatic(holder);
        final DynamicItemData dynamicData = resolveDynamic(holder, staticData);
        return new MenuItemData(staticData, dynamicData);
    }

    /**
     * Re-resolve only the dynamic side of a previously computed
     * {@link MenuItemData}, returning a new MenuItemData with the same static
     * side. Use this on the render path to keep placeholders fresh while
     * reusing the cached static.
     */
    public MenuItemData refreshDynamic(@NotNull final MenuHolder holder, @NotNull final MenuItemData template) {
        final StaticItemData staticData = template.getStaticData();
        final DynamicItemData dynamicData = resolveDynamic(holder, staticData);
        return new MenuItemData(staticData, dynamicData);
    }

    private StaticItemData getOrComputeStatic(@NotNull final MenuHolder holder) {
        if (this.options.caching()) {
            final UUID cacheUuid = this.options.shared() ? null : holder.getViewer().getUniqueId();
            final StaticItemData cached = plugin.getMenuCache().getItemStatic(
                    cacheUuid,
                    holder.getMenuName(),
                    this.configKey,
                    holder.getTypedArgs()
            );
            if (cached != null) return cached;
            final StaticItemData computed = computeStatic(holder);
            plugin.getMenuCache().putItemStatic(
                    cacheUuid,
                    holder.getMenuName(),
                    this.configKey,
                    holder.getTypedArgs(),
                    computed
            );
            return computed;
        }
        return computeStatic(holder);
    }

    private StaticItemData computeStatic(@NotNull final MenuHolder holder) {
        final String stringMaterial = this.options.material();
        final String lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ROOT);

        final boolean isDynamicMaterial = ItemUtils.isPlaceholderOption(lowercaseStringMaterial)
                || ItemUtils.isItemStackOption(lowercaseStringMaterial)
                || ItemUtils.isPlayerItem(lowercaseStringMaterial)
                || isHeadItem(lowercaseStringMaterial)
                || matchesHookPrefix(lowercaseStringMaterial);

        final Optional<Material> staticMaterial;
        final Optional<String> materialTemplate;
        if (isDynamicMaterial) {
            staticMaterial = Optional.empty();
            materialTemplate = Optional.of(stringMaterial);
        } else {
            final Material resolved = Material.getMaterial(stringMaterial.toUpperCase(Locale.ROOT));
            staticMaterial = Optional.of(resolved != null ? resolved : Material.STONE);
            materialTemplate = Optional.empty();
        }

        final boolean isWaterBottle = ItemUtils.isWaterBottle(lowercaseStringMaterial);
        final boolean isPlayerItem = ItemUtils.isPlayerItem(lowercaseStringMaterial);

        final ItemHook pluginHook = !isDynamicMaterial ? null : plugin.getItemHooks().values()
                .stream()
                .filter(x -> lowercaseStringMaterial.startsWith(x.getPrefix()))
                .findFirst()
                .orElse(null);

        final Optional<String> hookName = Optional.ofNullable(pluginHook).map(ItemHook::getPrefix);
        final Optional<String> base64Data = ItemUtils.isItemStackOption(lowercaseStringMaterial)
                ? Optional.of(stringMaterial)
                : Optional.empty();

        final int staticAmount;
        final Optional<String> amountTemplate;
        if (this.options.dynamicAmount().isPresent()) {
            staticAmount = -1;
            amountTemplate = this.options.dynamicAmount();
        } else {
            staticAmount = this.options.amount();
            amountTemplate = Optional.empty();
        }

        return new StaticItemData(
                this.options.slot(),
                this.options.priority(),
                staticMaterial.orElse(null),
                staticAmount,
                this.options.enchantments(),
                new ArrayList<>(this.options.itemFlags()),
                this.options.unbreakable(),
                new ArrayList<>(this.options.potionEffects()),
                isPlayerItem,
                isWaterBottle,
                combineNbtTemplates(this.options.nbtString(), this.options.nbtStrings()),
                combineNbtTemplates(this.options.nbtByte(), this.options.nbtBytes()),
                combineNbtTemplates(this.options.nbtShort(), this.options.nbtShorts()),
                combineNbtTemplates(this.options.nbtInt(), this.options.nbtInts()),
                hookName,
                base64Data,
                materialTemplate,
                amountTemplate,
                this.options.displayName(),
                this.options.lore(),
                this.options.customModelData(),
                this.options.rgb(),
                this.options.rarity(),
                this.options.hideTooltip(),
                this.options.enchantmentGlintOverride(),
                this.options.tooltipStyle(),
                this.options.itemModel(),
                this.options.trimMaterial(),
                this.options.trimPattern(),
                this.options.lightLevel(),
                this.options.damage(),
                pluginHook != null ? Optional.of(stringMaterial.substring(pluginHook.getPrefix().length())) : Optional.empty()
        );
    }

    private DynamicItemData resolveDynamic(@NotNull final MenuHolder holder, @NotNull final StaticItemData s) {
        final Player viewer = holder.getViewer();
        final String stringMaterial = this.options.material();
        final String lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ROOT);

        // Material resolution
        Optional<Material> material = Optional.empty();
        String hookArgs = null;
        if (s.getMaterialTemplate().isPresent()) {
            String resolved = s.getMaterialTemplate().get();
            final String initialLower = resolved.toLowerCase(Locale.ROOT);
            if (ItemUtils.isPlaceholderOption(initialLower)) {
                resolved = holder.setPlaceholdersAndArguments(resolved.substring(PLACEHOLDER_PREFIX.length()));
            }
            final String lower = resolved.toLowerCase(Locale.ROOT);
            if (ItemUtils.isItemStackOption(lower)) {
                resolved = holder.setPlaceholdersAndArguments(resolved.substring(STACK_PREFIX.length()));
                final ItemStack base64Item = base64ToItemStack(resolved);
                if (base64Item != null) {
                    material = Optional.of(base64Item.getType());
                }
            } else if (ItemUtils.isPlayerItem(lower)) {
                final ItemStack playerItem = INVENTORY_ITEM_ACCESSORS.get(lower).apply(viewer.getInventory());
                if (playerItem != null) {
                    material = Optional.of(playerItem.getType());
                } else {
                    material = Optional.of(Material.AIR);
                }
            } else {
                final ItemHook pluginHook = plugin.getItemHooks().values()
                        .stream()
                        .filter(x -> lower.startsWith(x.getPrefix()))
                        .findFirst()
                        .orElse(null);
                if (pluginHook != null) {
                    hookArgs = holder.setPlaceholdersAndArguments(resolved.substring(pluginHook.getPrefix().length()));
                } else if (!ItemUtils.isWaterBottle(lower)) {
                    final Material mat = Material.getMaterial(resolved.toUpperCase(Locale.ROOT));
                    if (mat != null) {
                        material = Optional.of(mat);
                    }
                }
            }
        }

        // Amount
        Optional<Integer> amount = Optional.empty();
        if (s.getAmountTemplate().isPresent()) {
            try {
                amount = Optional.of(Math.max((int) Double.parseDouble(holder.setPlaceholdersAndArguments(s.getAmountTemplate().get())), 1));
            } catch (final NumberFormatException ignored) {
            }
        }

        // Display name
        final Optional<String> displayName = s.getDisplayNameTemplate().map(holder::setPlaceholdersAndArguments);

        // Lore
        final List<String> lore = s.getLoreTemplates().isEmpty()
                ? Collections.emptyList()
                : getMenuItemLore(holder, s.getLoreTemplates());

        // Custom model data
        Optional<Integer> customModelData = Optional.empty();
        if (VersionHelper.IS_CUSTOM_MODEL_DATA && s.getCustomModelDataTemplate().isPresent()) {
            try {
                customModelData = Optional.of(Integer.parseInt(holder.setPlaceholdersAndArguments(s.getCustomModelDataTemplate().get())));
            } catch (final Exception ignored) {
            }
        }

        // RGB
        final Optional<Color> rgbColor = s.getRgbTemplate().map(holder::setPlaceholdersAndArguments).map(this::parseRGBColor);

        // Rarity
        Optional<ItemRarity> rarity = Optional.empty();
        if (VersionHelper.HAS_DATA_COMPONENTS && s.getRarityTemplate().isPresent()) {
            final String r = holder.setPlaceholdersAndArguments(s.getRarityTemplate().get());
            try {
                rarity = Optional.of(ItemRarity.valueOf(r.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        final Optional<Boolean> hideTooltip = s.getHideTooltipTemplate().map(holder::setPlaceholdersAndArguments).map(Boolean::parseBoolean);
        final Optional<Boolean> enchantmentGlintOverride = s.getEnchantmentGlintOverrideTemplate().map(holder::setPlaceholdersAndArguments).map(Boolean::parseBoolean);
        final Optional<NamespacedKey> tooltipStyle = s.getTooltipStyleTemplate().map(holder::setPlaceholdersAndArguments).map(NamespacedKey::fromString);
        final Optional<NamespacedKey> itemModel = s.getItemModelTemplate().map(holder::setPlaceholdersAndArguments).map(NamespacedKey::fromString);

        Optional<TrimMaterial> trimMaterial = Optional.empty();
        Optional<TrimPattern> trimPattern = Optional.empty();
        if (VersionHelper.HAS_ARMOR_TRIMS) {
            trimMaterial = s.getTrimMaterialTemplate().map(holder::setPlaceholdersAndArguments).map(Registry.TRIM_MATERIAL::match);
            trimPattern = s.getTrimPatternTemplate().map(holder::setPlaceholdersAndArguments).map(Registry.TRIM_PATTERN::match);
        }

        Optional<Integer> lightLevel = Optional.empty();
        if (s.getLightLevelTemplate().isPresent()) {
            try {
                lightLevel = Optional.of(Integer.parseInt(holder.setPlaceholdersAndArguments(s.getLightLevelTemplate().get())));
            } catch (Exception ignored) {
            }
        }

        short damage = 0;
        if (s.getDamageTemplate().isPresent()) {
            try {
                damage = (short) Integer.parseInt(holder.setPlaceholdersAndArguments(s.getDamageTemplate().get()));
            } catch (NumberFormatException ignored) {
            }
        }

        // NBT
        final Map<String, Object> nbtTags = new HashMap<>();
        nbtTags.putAll(resolveNbtStrings(holder, s.getNbtStringTemplates(), "s:"));
        nbtTags.putAll(resolveNbtBytes(holder, s.getNbtByteTemplates(), "b:"));
        nbtTags.putAll(resolveNbtShorts(holder, s.getNbtShortTemplates(), "sh:"));
        nbtTags.putAll(resolveNbtInts(holder, s.getNbtIntTemplates(), "i:"));

        return new DynamicItemData(
                material,
                amount,
                displayName,
                lore,
                customModelData,
                rgbColor,
                rarity,
                hideTooltip,
                enchantmentGlintOverride,
                tooltipStyle,
                itemModel,
                trimMaterial,
                trimPattern,
                lightLevel,
                damage,
                Optional.ofNullable(hookArgs),
                nbtTags
        );
    }

    private Map<String, Object> resolveNbtStrings(@NotNull final MenuHolder holder, @NotNull final List<String> templates, final String prefix) {
        final Map<String, Object> result = new HashMap<>();
        for (final String t : templates) {
            final String[] parts = t.split(":", 2);
            if (parts.length == 2) {
                result.put(prefix + parts[0], holder.setPlaceholdersAndArguments(parts[1]));
            }
        }
        return result;
    }

    private Map<String, Object> resolveNbtBytes(@NotNull final MenuHolder holder, @NotNull final List<String> templates, final String prefix) {
        final Map<String, Object> result = new HashMap<>();
        for (final String t : templates) {
            final String[] parts = t.split(":", 2);
            if (parts.length == 2) {
                try {
                    result.put(prefix + parts[0], Byte.parseByte(holder.setPlaceholdersAndArguments(parts[1])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private Map<String, Object> resolveNbtShorts(@NotNull final MenuHolder holder, @NotNull final List<String> templates, final String prefix) {
        final Map<String, Object> result = new HashMap<>();
        for (final String t : templates) {
            final String[] parts = t.split(":", 2);
            if (parts.length == 2) {
                try {
                    result.put(prefix + parts[0], Short.parseShort(holder.setPlaceholdersAndArguments(parts[1])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private Map<String, Object> resolveNbtInts(@NotNull final MenuHolder holder, @NotNull final List<String> templates, final String prefix) {
        final Map<String, Object> result = new HashMap<>();
        for (final String t : templates) {
            final String[] parts = t.split(":", 2);
            if (parts.length == 2) {
                try {
                    result.put(prefix + parts[0], Integer.parseInt(holder.setPlaceholdersAndArguments(parts[1])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private static List<String> nbtListOrEmpty(@NotNull final Optional<String> single) {
        return single.isPresent() ? List.of(single.get()) : Collections.emptyList();
    }

    private static List<String> combineNbtTemplates(@NotNull final Optional<String> single, @NotNull final List<String> list) {
        if (single.isEmpty()) return list;
        if (list.isEmpty()) return List.of(single.get());
        final List<String> result = new ArrayList<>(list.size() + 1);
        result.addAll(list);
        result.add(single.get());
        return result;
    }

    private boolean matchesHookPrefix(@NotNull final String lowerMaterial) {
        return plugin.getItemHooks().values().stream().anyMatch(x -> lowerMaterial.startsWith(x.getPrefix()));
    }

    public ItemStack applyDataToItemStack(@NotNull final MenuItemData data, @NotNull final ItemStack itemStack, @NotNull final Player viewer) {
        if (itemStack.getType() != data.getMaterial()) {
            itemStack.setType(data.getMaterial());
        }
        itemStack.setAmount(data.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        data.getDisplayName().ifPresent(name -> meta.setDisplayName(StringUtils.color(name)));
        if (!data.getLore().isEmpty()) {
            meta.setLore(StringUtils.color(data.getLore()));
        }

        itemStack.setItemMeta(meta);

        ItemStack result = itemStack;
        // NBT Support
        if (NbtProvider.isAvailable() && !data.getNbtTags().isEmpty()) {
            for (Map.Entry<String, Object> entry : data.getNbtTags().entrySet()) {
                String keyWithType = entry.getKey();
                Object value = entry.getValue();

                if (keyWithType.startsWith("s:")) {
                    result = NbtProvider.setString(result, keyWithType.substring(2), (String) value);
                } else if (keyWithType.startsWith("b:")) {
                    result = NbtProvider.setByte(result, keyWithType.substring(2), (Byte) value);
                } else if (keyWithType.startsWith("sh:")) {
                    result = NbtProvider.setShort(result, keyWithType.substring(3), (Short) value);
                } else if (keyWithType.startsWith("i:")) {
                    result = NbtProvider.setInt(result, keyWithType.substring(2), (Integer) value);
                }
            }
        }
        return result;
    }

    public ItemStack getItemStack(@NotNull final MenuItemData data, @NotNull final Player viewer) {
        ItemStack itemStack = null;

        if (data.getHookName().isPresent()) {
            itemStack = plugin.getItemHook(data.getHookName().get())
                    .map(hook -> hook.getItem(viewer, data.getHookArgs().orElse("")))
                    .orElse(null);
        }

        if (itemStack == null && data.getBase64Data().isPresent()) {
            itemStack = base64ToItemStack(data.getBase64Data().get());
        }

        if (itemStack == null && data.isPlayerItem()) {
            final String lowercaseStringMaterial = this.options.material().toLowerCase(Locale.ROOT);
            final ItemStack playerItem = INVENTORY_ITEM_ACCESSORS.get(lowercaseStringMaterial).apply(viewer.getInventory());
            if (playerItem != null) {
                itemStack = playerItem.clone();
            }
        }

        if (itemStack == null && data.isWaterBottle()) {
            itemStack = ItemUtils.createWaterBottles(data.getAmount());
        }

        if (itemStack == null) {
            itemStack = new ItemStack(data.getMaterial(), data.getAmount());
        }

        if (itemStack.getType() == Material.AIR) {
            return itemStack;
        }

        itemStack.setAmount(data.getAmount());

        if (ItemUtils.isBanner(itemStack.getType())) {
            final BannerMeta meta = (BannerMeta) itemStack.getItemMeta();
            if (meta != null) {
                if (!this.options.bannerMeta().isEmpty()) {
                    meta.setPatterns(this.options.bannerMeta());
                }
                itemStack.setItemMeta(meta);
            }
        }

        if (ItemUtils.isShield(itemStack.getType())) {
            final BlockStateMeta blockStateMeta = (BlockStateMeta) itemStack.getItemMeta();
            if (blockStateMeta != null) {
                final Banner banner = (Banner) blockStateMeta.getBlockState();
                this.options.baseColor().ifPresent(banner::setBaseColor);
                if (!this.options.bannerMeta().isEmpty()) {
                    banner.setPatterns(this.options.bannerMeta());
                }
                banner.update();
                blockStateMeta.setBlockState(banner);
                itemStack.setItemMeta(blockStateMeta);
            }
        }

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return itemStack;

        if (ItemUtils.hasPotionMeta(itemStack)) {
            final PotionMeta meta = (PotionMeta) itemMeta;
            data.getRgbColor().ifPresent(meta::setColor);
            for (PotionEffect effect : data.getPotionEffects()) {
                meta.addCustomEffect(effect, true);
            }
        }

        if (itemMeta instanceof Damageable && data.getDamage() > 0) {
            ((Damageable) itemMeta).setDamage(data.getDamage());
        }

        data.getCustomModelData().ifPresent(itemMeta::setCustomModelData);
        data.getDisplayName().ifPresent(name -> itemMeta.setDisplayName(StringUtils.color(name)));

        // Lore handling
        List<String> finalLore = new ArrayList<>();
        List<String> itemLore = Objects.requireNonNullElse(itemMeta.getLore(), new ArrayList<>());
        LoreAppendMode mode = this.options.loreAppendMode().orElse(LoreAppendMode.OVERRIDE);
        if (!this.options.hasLore() && this.options.loreAppendMode().isEmpty()) mode = LoreAppendMode.IGNORE;

        switch (mode) {
            case IGNORE: finalLore.addAll(itemLore); break;
            case TOP: finalLore.addAll(data.getLore()); finalLore.addAll(itemLore); break;
            case BOTTOM: finalLore.addAll(itemLore); finalLore.addAll(data.getLore()); break;
            case OVERRIDE: finalLore.addAll(data.getLore()); break;
        }
        itemMeta.setLore(finalLore);

        if (data.isUnbreakable()) itemMeta.setUnbreakable(true);

        if (VersionHelper.HAS_DATA_COMPONENTS) {
            data.getHideTooltip().ifPresent(itemMeta::setHideTooltip);
            data.getEnchantmentGlintOverride().ifPresent(itemMeta::setEnchantmentGlintOverride);
            data.getRarity().ifPresent(itemMeta::setRarity);
        }

        if (VersionHelper.HAS_TOOLTIP_STYLE) {
            data.getTooltipStyle().ifPresent(itemMeta::setTooltipStyle);
            data.getItemModel().ifPresent(itemMeta::setItemModel);
        }

        if (VersionHelper.HAS_ARMOR_TRIMS && ItemUtils.hasArmorMeta(itemStack)) {
            if (data.getTrimMaterial().isPresent() && data.getTrimPattern().isPresent()) {
                final ArmorTrim armorTrim = new ArmorTrim(data.getTrimMaterial().get(), data.getTrimPattern().get());
                ((ArmorMeta) itemMeta).setTrim(armorTrim);
            }
        }

        if (itemMeta instanceof LeatherArmorMeta) {
            data.getRgbColor().ifPresent(((LeatherArmorMeta) itemMeta)::setColor);
        } else if (itemMeta instanceof FireworkEffectMeta) {
            data.getRgbColor().ifPresent(c -> ((FireworkEffectMeta) itemMeta).setEffect(FireworkEffect.builder().withColor(c).build()));
        }

        if (itemMeta instanceof EnchantmentStorageMeta) {
            data.getEnchantments().forEach((e, l) -> ((EnchantmentStorageMeta) itemMeta).addStoredEnchant(e, l, true));
        } else {
            data.getEnchantments().forEach((e, l) -> itemMeta.addEnchant(e, l, true));
        }

        if (data.getLightLevel().isPresent() && itemMeta instanceof BlockDataMeta) {
            final BlockDataMeta blockDataMeta = (BlockDataMeta) itemMeta;
            final BlockData blockData = blockDataMeta.getBlockData(itemStack.getType());
            if (blockData instanceof Light) {
                final Light light = (Light) blockData;
                light.setLevel(Math.max(0, Math.min(data.getLightLevel().get(), light.getMaximumLevel())));
                blockDataMeta.setBlockData(light);
            }
        }

        for (final ItemFlag flag : data.getItemFlags()) {
            itemMeta.addItemFlags(flag);
            if (flag == ItemFlag.HIDE_ATTRIBUTES && VersionHelper.HAS_DATA_COMPONENTS) {
                itemMeta.setAttributeModifiers(ImmutableMultimap.of());
            }
        }

        itemStack.setItemMeta(itemMeta);

        // NBT Support
        if (NbtProvider.isAvailable() && !data.getNbtTags().isEmpty()) {
            for (Map.Entry<String, Object> entry : data.getNbtTags().entrySet()) {
                String keyWithType = entry.getKey();
                Object value = entry.getValue();

                if (keyWithType.startsWith("s:")) {
                    itemStack = NbtProvider.setString(itemStack, keyWithType.substring(2), (String) value);
                } else if (keyWithType.startsWith("b:")) {
                    itemStack = NbtProvider.setByte(itemStack, keyWithType.substring(2), (Byte) value);
                } else if (keyWithType.startsWith("sh:")) {
                    itemStack = NbtProvider.setShort(itemStack, keyWithType.substring(3), (Short) value);
                } else if (keyWithType.startsWith("i:")) {
                    itemStack = NbtProvider.setInt(itemStack, keyWithType.substring(2), (Integer) value);
                }
            }
        }

        return itemStack;
    }

    public ItemStack getItemStack(@NotNull final MenuHolder holder) {
        MenuItemData data = computeData(holder);
        return getItemStack(data, holder.getViewer());
    }

    /**
     * Checks if the string is a head item. The check is case-insensitive.
     * Head items are:
     * <ul>
     * <li>"head-{player-name}" (a simple named player head, supports placeholders. eg. "head-%player_name% or head-extendedclip")</li>
     * <li>"texture-{texture-url}" (a head with a custom texture specified by a texture url. eg. "texture-93a728ad8d31486a7f9aad200edb373ea803d1fc5fd4321b2e2a971348234443")</li>
     * <li>"basehead-{base64-encoded-texture-url}" (a head with a base64 encoded texture url)</li>
     * <li>"hdb-{hdb-head-id}" (a head with a custom texture specified by a <a href="https://www.spigotmc.org/resources/14280/">HeadDatabase</a> id)</li>
     * </ul>
     *
     * @param material The string to check
     * @return true if the string is a head item, false otherwise
     */
    private boolean isHeadItem(@NotNull final String material) {
        final Optional<HeadType> headType = HeadType.parseHeadType(material);
        headType.ifPresent(this.options::headType);
        return headType.isPresent();
    }

    private @NotNull Optional<ItemStack> getItemFromHook(String hookName, String... args) {
        return plugin.getItemHook(hookName).map(itemHook -> itemHook.getItem(args));
    }

    protected List<String> getMenuItemLore(@NotNull final MenuHolder holder, @NotNull final List<String> lore) {
        return lore.stream()
                .map(holder::setPlaceholdersAndArguments)
                .map(StringUtils::color)
                .map(line -> line.split("\n"))
                .flatMap(Arrays::stream)
                .map(line -> line.split("\\\\n"))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    private @NotNull org.bukkit.inventory.meta.components.CustomModelDataComponent parseCustomModelDataComponent(
            @NotNull final CustomModelDataComponent unparsedComponent,
            @NotNull final org.bukkit.inventory.meta.components.CustomModelDataComponent component,
            @NotNull final MenuHolder holder
    ) {
        if (!unparsedComponent.colors().isEmpty()) {
            final List<Color> colors = unparsedComponent.colors()
                    .stream()
                    .map(holder::setPlaceholdersAndArguments)
                    .map(this::parseRGBColor)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            component.setColors(colors);
        }

        if (!unparsedComponent.flags().isEmpty()) {
            final List<Boolean> flags = unparsedComponent.flags()
                    .stream()
                    .map(holder::setPlaceholdersAndArguments)
                    .map(Boolean::parseBoolean)
                    .collect(Collectors.toList());
            component.setFlags(flags);
        }

        if (!unparsedComponent.floats().isEmpty()) {
            final List<Float> floats = unparsedComponent.floats()
                    .stream()
                    .map(holder::setPlaceholdersAndArguments)
                    .map(Float::parseFloat)
                    .collect(Collectors.toList());
            component.setFloats(floats);
        }

        if (!unparsedComponent.strings().isEmpty()) {
            final List<String> strings = unparsedComponent.strings()
                    .stream()
                    .map(holder::setPlaceholdersAndArguments)
                    .collect(Collectors.toList());
            component.setStrings(strings);
        }

        return component;
    }

    private @Nullable Color parseRGBColor(@NotNull final String input) {
        final Color color = StringUtils.parseRGBColor(input);
        if (color == null) {
            plugin.debug(
                    DebugLevel.HIGHEST,
                    Level.WARNING,
                    "Invalid RGB color found: " + input
            );
        }
        return color;
    }

    public @NotNull MenuItemOptions options() {
        return options;
    }
}
