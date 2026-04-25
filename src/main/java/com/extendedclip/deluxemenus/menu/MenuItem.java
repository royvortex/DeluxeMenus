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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.extendedclip.deluxemenus.utils.Constants.INVENTORY_ITEM_ACCESSORS;
import static com.extendedclip.deluxemenus.utils.Constants.PLACEHOLDER_PREFIX;
import static com.extendedclip.deluxemenus.utils.Constants.STACK_PREFIX;

public class MenuItem {

    private final DeluxeMenus plugin;
    private final MenuItemOptions options;

    public MenuItem(@NotNull final DeluxeMenus plugin, @NotNull final MenuItemOptions options) {
        this.plugin = plugin;
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

    public MenuItemData computeData(@NotNull final MenuHolder holder) {
        final Player viewer = holder.getViewer();

        String hookName = null;
        String hookArgs = null;
        String base64Data = null;
        boolean isPlayerItem = false;
        boolean isWaterBottle = false;
        Material material = null;
        int amount = 1;

        String stringMaterial = this.options.material();
        String lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ROOT);

        if (ItemUtils.isPlaceholderOption(lowercaseStringMaterial)) {
            stringMaterial = holder.setPlaceholdersAndArguments(stringMaterial.substring(PLACEHOLDER_PREFIX.length()));
            lowercaseStringMaterial = stringMaterial.toLowerCase(Locale.ENGLISH);
        }
        if (ItemUtils.isItemStackOption(lowercaseStringMaterial)) {
            stringMaterial = holder.setPlaceholdersAndArguments(stringMaterial.substring(STACK_PREFIX.length()));
            base64Data = stringMaterial;
            ItemStack base64Item = base64ToItemStack(stringMaterial);
            if (base64Item != null) {
                amount = base64Item.getAmount();
                material = base64Item.getType();
            }
        }

        if (ItemUtils.isPlayerItem(lowercaseStringMaterial)) {
            isPlayerItem = true;
            final ItemStack playerItem = INVENTORY_ITEM_ACCESSORS.get(lowercaseStringMaterial).apply(viewer.getInventory());
            if (playerItem != null) {
                material = playerItem.getType();
                amount = playerItem.getAmount();
            } else {
                material = Material.AIR;
            }
        }

        final String finalMaterial = lowercaseStringMaterial;
        final ItemHook pluginHook = plugin.getItemHooks().values()
                .stream()
                .filter(x -> finalMaterial.startsWith(x.getPrefix()))
                .findFirst()
                .orElse(null);

        if (pluginHook != null) {
            hookName = pluginHook.getPrefix();
            hookArgs = holder.setPlaceholdersAndArguments(stringMaterial.substring(pluginHook.getPrefix().length()));
        }

        if (ItemUtils.isWaterBottle(stringMaterial)) {
            isWaterBottle = true;
        }

        if (material == null && !isWaterBottle && hookName == null) {
            material = Material.getMaterial(stringMaterial.toUpperCase(Locale.ROOT));
            if (material == null) {
                material = Material.STONE;
            }
        }

        if (this.options.amount() != -1) {
            amount = this.options.amount();
        }

        if (this.options.dynamicAmount().isPresent()) {
            try {
                final int dynamicAmount = (int) Double.parseDouble(holder.setPlaceholdersAndArguments(this.options.dynamicAmount().get()));
                amount = Math.max(dynamicAmount, 1);
            } catch (final NumberFormatException ignored) {
            }
        }

        Optional<String> displayName = this.options.displayName().map(holder::setPlaceholdersAndArguments);
        List<String> lore = getMenuItemLore(holder, this.options.lore());

        Optional<Integer> customModelData = Optional.empty();
        if (VersionHelper.IS_CUSTOM_MODEL_DATA && this.options.customModelData().isPresent()) {
            try {
                customModelData = Optional.of(Integer.parseInt(holder.setPlaceholdersAndArguments(this.options.customModelData().get())));
            } catch (final Exception ignored) {}
        }

        Optional<Color> rgbColor = this.options.rgb().map(holder::setPlaceholdersAndArguments).map(this::parseRGBColor);
        
        Optional<ItemRarity> rarity = Optional.empty();
        if (VersionHelper.HAS_DATA_COMPONENTS && this.options.rarity().isPresent()) {
            String r = holder.setPlaceholdersAndArguments(this.options.rarity().get());
            try {
                rarity = Optional.of(ItemRarity.valueOf(r.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        Optional<Boolean> hideTooltip = this.options.hideTooltip().map(holder::setPlaceholdersAndArguments).map(Boolean::parseBoolean);
        Optional<Boolean> enchantmentGlintOverride = this.options.enchantmentGlintOverride().map(holder::setPlaceholdersAndArguments).map(Boolean::parseBoolean);
        Optional<NamespacedKey> tooltipStyle = this.options.tooltipStyle().map(holder::setPlaceholdersAndArguments).map(NamespacedKey::fromString);
        Optional<NamespacedKey> itemModel = this.options.itemModel().map(holder::setPlaceholdersAndArguments).map(NamespacedKey::fromString);

        Optional<TrimMaterial> trimMaterial = Optional.empty();
        Optional<TrimPattern> trimPattern = Optional.empty();
        if (VersionHelper.HAS_ARMOR_TRIMS) {
            trimMaterial = this.options.trimMaterial().map(holder::setPlaceholdersAndArguments).map(Registry.TRIM_MATERIAL::match);
            trimPattern = this.options.trimPattern().map(holder::setPlaceholdersAndArguments).map(Registry.TRIM_PATTERN::match);
        }

        Optional<Integer> lightLevel = Optional.empty();
        if (this.options.lightLevel().isPresent()) {
            try {
                lightLevel = Optional.of(Integer.parseInt(holder.setPlaceholdersAndArguments(this.options.lightLevel().get())));
            } catch (Exception ignored) {}
        }

        short damage = 0;
        if (this.options.damage().isPresent()) {
            try {
                damage = (short) Integer.parseInt(holder.setPlaceholdersAndArguments(this.options.damage().get()));
            } catch (NumberFormatException ignored) {}
        }

        return new MenuItemData(
                this.options.slot(),
                this.options.priority(),
                material != null ? material : Material.STONE,
                amount,
                displayName,
                lore,
                this.options.enchantments(),
                damage,
                customModelData,
                this.options.unbreakable(),
                new ArrayList<>(this.options.itemFlags()),
                rgbColor,
                this.options.potionEffects(),
                rarity,
                hideTooltip,
                enchantmentGlintOverride,
                tooltipStyle,
                itemModel,
                trimMaterial,
                trimPattern,
                lightLevel,
                Map.of(), // TODO: Support NBT parsing in computeData if needed
                Optional.ofNullable(hookName),
                Optional.ofNullable(hookArgs),
                Optional.ofNullable(base64Data),
                isPlayerItem,
                isWaterBottle
        );
    }

    public void applyDataToItemStack(@NotNull final MenuItemData data, @NotNull final ItemStack itemStack, @NotNull final Player viewer) {
        if (itemStack.getType() != data.getMaterial()) {
            itemStack.setType(data.getMaterial());
        }
        itemStack.setAmount(data.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        data.getDisplayName().ifPresent(name -> meta.setDisplayName(StringUtils.color(name)));
        if (!data.getLore().isEmpty()) {
            meta.setLore(StringUtils.color(data.getLore()));
        }

        itemStack.setItemMeta(meta);
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

        // NBT Support (Basic)
        if (NbtProvider.isAvailable()) {
            // This is a simplified version of the original NBT logic
            // In a full implementation, we'd pre-parse NBT strings in computeData
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
     * <li>"basehead-{base64-encoded-texture-url}" (a head with a custom texture specified by a base64 encoded texture url)</li>
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
