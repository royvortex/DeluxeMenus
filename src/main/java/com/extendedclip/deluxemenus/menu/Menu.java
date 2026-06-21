package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.events.DeluxeMenusOpenMenuEvent;
import com.extendedclip.deluxemenus.events.DeluxeMenusPreOpenMenuEvent;
import com.extendedclip.deluxemenus.menu.command.RegistrableMenuCommand;
import com.extendedclip.deluxemenus.menu.options.MenuOptions;
import com.extendedclip.deluxemenus.requirement.RequirementList;
import com.extendedclip.deluxemenus.scheduler.scheduling.schedulers.TaskScheduler;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Menu {

    private static final Map<String, Menu> menus = new ConcurrentHashMap<>();
    private static final Set<MenuHolder> menuHolders = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Menu> lastOpenedMenus = new ConcurrentHashMap<>();

    private final DeluxeMenus plugin;
    private final TaskScheduler scheduler;
    private final MenuOptions options;
    private final Map<Integer, TreeMap<Integer, MenuItem>> items;
    // menu path starting from the plugin directory
    private final String path;

    private RegistrableMenuCommand command = null;

    public Menu(
            final @NotNull DeluxeMenus plugin,
            final @NotNull MenuOptions options,
            final @NotNull Map<Integer, TreeMap<Integer, MenuItem>> items,
            final @NotNull String path
    ) {
        this.plugin = plugin;
        this.scheduler = plugin.getScheduler();
        this.options = options;
        this.items = items;
        this.path = path;

        if (this.options.registerCommands()) {
            this.command = new RegistrableMenuCommand(plugin, this);
            this.command.register();
        }

        menus.put(this.options.name(), this);
    }

    public static void unload(final @NotNull DeluxeMenus plugin, final @NotNull String name) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInMenu(p, name)) {
                closeMenu(plugin, p, true);
            }
        }

        Optional<Menu> optionalMenu = Menu.getMenuByName(name);
        if (optionalMenu.isEmpty()) {
            return;
        }

        optionalMenu.get().unregisterCommand();
        menus.remove(name);
    }

    public static void unload(final @NotNull DeluxeMenus plugin) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInMenu(p)) {
                closeMenu(plugin, p, true);
            }
        }
        for (Menu menu : Menu.getAllMenus()) {
            menu.unregisterCommand();
        }
        menus.clear();
        menuHolders.clear();
        lastOpenedMenus.clear();
    }

    private void unregisterCommand() {
        if (this.command != null) {
            this.command.unregister();
        }

        // WARNING! A reference to the command is stored by CraftBukkit for their `/help` command. There is currently
        // no way to remove this reference!
        this.command = null;
    }

    public static void unloadForShutdown(final @NotNull DeluxeMenus plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInMenu(player)) {
                closeMenuForShutdown(plugin, player);
            }
        }
        menus.clear();
    }

    public static int getLoadedMenuSize() {
        return menus.size();
    }

    public static @NotNull Set<String> getAllMenuNames() {
        return menus.keySet();
    }

    public static @NotNull Collection<Menu> getAllMenus() {
        return menus.values();
    }

    // Menus need to be stored in a list because config.yml can contain multiple menus.
    // This can be changed once we remove support for menus inside the config file.
    public static @NotNull TreeMap<String, List<Menu>> getPathSortedMenus() {
        return menus.values().stream().map(m -> Map.entry(m.path(), m)).collect(
                TreeMap::new, (tree, entry) -> {
                    final List<Menu> list = tree.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                    list.add(entry.getValue());
                    tree.put(entry.getKey(), list);
                },
                (tree1, tree2) -> {
                    for (Entry<String, List<Menu>> entry : tree2.entrySet()) {
                        final List<Menu> list = tree1.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                        list.addAll(entry.getValue());
                        tree1.put(entry.getKey(), list);
                    }
                }
        );
    }

    public static @NotNull Optional<Menu> getMenuByName(final @NotNull String name) {
        return menus.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)).findFirst().map(Entry::getValue);
    }

    public static @NotNull Optional<Menu> getMenuByCommand(final @NotNull String command) {
        return menus.values().stream().filter(m -> m.getMenuCommandUsed(command).isPresent()).findFirst();
    }

    public static boolean isMenuCommand(final @NotNull String command) {
        return getMenuByCommand(command).isPresent();
    }

    public static boolean isInMenu(final @NotNull Player player) {
        return menuHolders.stream().anyMatch(h -> h.getViewerName().equals(player.getName()));
    }

    public static boolean isInMenu(final @NotNull Player player, final @NotNull String menu) {
        return menuHolders.stream().anyMatch(h -> h.getMenuName().equals(menu) && h.getViewerName().equals(player.getName()));
    }

    public static Optional<MenuHolder> getMenuHolder(final @NotNull Player player) {
        return menuHolders.stream().filter(h -> h.getViewerName().equals(player.getName())).findFirst();
    }

    public static Optional<Menu> getOpenMenu(final @NotNull Player player) {
        return getMenuHolder(player).flatMap(MenuHolder::getMenu);
    }

    public static Optional<Menu> getLastMenu(final @NotNull Player player) {
        return Optional.ofNullable(lastOpenedMenus.get(player.getUniqueId()));
    }

    public static void cleanInventory(final @NotNull DeluxeMenus plugin, final @NotNull Player player) {
        for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null) continue;
            if (!plugin.getMenuItemMarker().isMarked(itemStack)) continue;

            plugin.debug(
                    DebugLevel.LOWEST,
                    Level.INFO,
                    "Found a DeluxeMenus item in a player's inventory. Removing it."
            );
            player.getInventory().remove(itemStack);
        }
        player.updateInventory();
    }

    public static void closeMenu(final @NotNull DeluxeMenus plugin, final @NotNull Player player, final boolean close, final boolean executeCloseActions) {
        Optional<MenuHolder> optionalHolder = getMenuHolder(player);
        if (optionalHolder.isEmpty()) {
            return;
        }

        MenuHolder holder = optionalHolder.get();

        holder.stopPlaceholderUpdate();
        holder.stopRefreshTask();

        if (executeCloseActions) {
            holder.getMenu().map(Menu::options).map(MenuOptions::closeHandler).flatMap(h -> h).ifPresent(h -> h.onClick(holder));
        }

        if (close) {
            plugin.getScheduler().runTask(player, () -> {
                player.closeInventory();
                cleanInventory(plugin, player);
            });
        }
        menuHolders.remove(holder);
        lastOpenedMenus.put(player.getUniqueId(), holder.getMenu().orElse(null));
    }

    public static void closeMenuForShutdown(final @NotNull DeluxeMenus plugin, final @NotNull Player player) {
        getMenuHolder(player).ifPresent(MenuHolder::stopPlaceholderUpdate);

        player.closeInventory();
        cleanInventory(plugin, player);
    }

    public static void closeMenu(final @NotNull DeluxeMenus plugin, final @NotNull Player player, final boolean close) {
        closeMenu(plugin, player, close, false);
    }

    private boolean hasOpenBypassPerm(final @NotNull Player viewer) {
        return viewer.hasPermission("deluxemenus.openrequirement.bypass." + this.options.name())
                || viewer.hasPermission("deluxemenus.openrequirement.bypass.*");
    }

    private boolean handleOpenRequirements(final @NotNull MenuHolder holder) {
        if (this.options.openRequirements().isEmpty()) {
            return true;
        }

        final RequirementList openRequirements = this.options.openRequirements().get();
        if (openRequirements.getRequirements() == null) {
            return true;
        }

        if (holder.getViewer() != null && (this.options.enableBypassPerm() && this.hasOpenBypassPerm(holder.getViewer()))) {
            return true;
        }

        if (!openRequirements.evaluate(holder)) {
            if (openRequirements.getDenyHandler() != null) {
                openRequirements.getDenyHandler().onClick(holder);
            }
            return false;
        }
        return true;
    }

    private boolean handleArgRequirements(final @NotNull MenuHolder holder) {
        for (RequirementList rl : this.options.argumentRequirements()) {
            if (rl.getRequirements() == null) {
                continue;
            }

            if (!rl.evaluate(holder)) {
                if (rl.getDenyHandler() != null) {
                    rl.getDenyHandler().onClick(holder);
                }
                return false;
            }
        }

        return true;
    }

    public void openMenu(final @NotNull Player viewer) {
        openMenu(viewer, null, null);
    }

    public void openMenu(final @NotNull Player viewer, final @Nullable Map<String, String> args, final @Nullable Player placeholderPlayer) {
        // On Folia, Bukkit.createInventory (called inside renderSnapshot) requires
        // the entity's region thread. Click actions run on the global region
        // scheduler, so without this guard openMenu would crash with "Cannot init
        // menu async" when a click command reopens a menu. On Paper/Bukkit,
        // isEntityThread returns isPrimaryThread, which is also correct.
        if (!scheduler.isEntityThread(viewer)) {
            scheduler.runTask(viewer, () -> openMenu(viewer, args, placeholderPlayer));
            return;
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        DeluxeMenusPreOpenMenuEvent preOpenEvent = new DeluxeMenusPreOpenMenuEvent(viewer);
        Bukkit.getPluginManager().callEvent(preOpenEvent);

        if (preOpenEvent.isCancelled()) {
            return;
        }

        final MenuHolder holder = new MenuHolder(plugin, viewer);
        holder.setMenuName(this.options.name());
        if (placeholderPlayer != null) {
            holder.setPlaceholderPlayer(placeholderPlayer);
        }
        holder.setTypedArgs(args);
        holder.parsePlaceholdersInArguments(this.options.parsePlaceholdersInArguments());
        holder.parsePlaceholdersAfterArguments(this.options.parsePlaceholdersAfterArguments());

        if (!this.handleArgRequirements(holder)) {
            return;
        }

        if (!this.handleOpenRequirements(holder)) {
            return;
        }

        // Check Cache
        final UUID cacheUuid = this.options.shared() ? null : viewer.getUniqueId();
        MenuSnapshot cachedSnapshot = plugin.getMenuCache().get(cacheUuid, this.options.name(), args);
        if (cachedSnapshot != null) {
            renderSnapshot(holder, cachedSnapshot, viewer);
            return;
        }

        scheduler.runTaskAsynchronously(() -> {
            MenuSnapshot snapshot = computeSnapshot(holder);
            if (snapshot == null) return;

            // Check if any items need placeholder updates
            boolean updatePlaceholders = snapshot.getItems().stream()
                    .anyMatch(data -> items.get(data.getSlot()).get(data.getPriority()).options().updatePlaceholders());

            // Put in Cache
            plugin.getMenuCache().put(cacheUuid, this.options.name(), args, snapshot);

            final boolean finalUpdatePlaceholders = updatePlaceholders;

            // Switch to Main Thread for Rendering
            scheduler.runTask(viewer, () -> renderSnapshot(holder, snapshot, viewer, finalUpdatePlaceholders));
        });
    }

    public @Nullable MenuSnapshot computeSnapshot(@NotNull final MenuHolder holder) {
        List<MenuItemData> itemDataList = new ArrayList<>();

        for (Entry<Integer, TreeMap<Integer, MenuItem>> entry : items.entrySet()) {
            for (MenuItem item : entry.getValue().values()) {
                int slot = item.options().slot();

                if (slot >= this.options.size()) {
                    continue;
                }

                if (item.options().viewRequirements().isPresent()) {
                    if (item.options().viewRequirements().get().evaluate(holder)) {
                        itemDataList.add(item.computeData(holder));
                        break;
                    }
                } else {
                    itemDataList.add(item.computeData(holder));
                    break;
                }
            }
        }

        if (itemDataList.isEmpty()) {
            return null;
        }

        String title = StringUtils.color(holder.setPlaceholdersAndArguments(this.options.title()));
        return new MenuSnapshot(title, this.options.size(), this.options.type(), itemDataList);
    }

    private void renderSnapshot(final @NotNull MenuHolder holder, final @NotNull MenuSnapshot snapshot, final @NotNull Player viewer) {
        renderSnapshot(holder, snapshot, viewer, false);
    }

    private void renderSnapshot(final @NotNull MenuHolder holder, final @NotNull MenuSnapshot snapshot, final @NotNull Player viewer, final boolean updatePlaceholders) {
        holder.setMenuName(this.options.name());

        this.options.openHandler().ifPresent(h -> h.onClick(holder));

        Inventory inventory;
        if (snapshot.getType() != InventoryType.CHEST) {
            inventory = Bukkit.createInventory(holder, snapshot.getType(), snapshot.getTitle());
        } else {
            inventory = Bukkit.createInventory(holder, snapshot.getSize(), snapshot.getTitle());
        }
        holder.setInventory(inventory);

        renderSnapshotToInventory(holder, snapshot, inventory);

        // Populate the holder's activeItems so PlayerListener.onClick can resolve
        // a clicked slot back to the MenuItem that owns it. Lost during the
        // static/dynamic refactor; MenuHolder.refreshMenu still does this, but the
        // menu-open path was missing it.
        final Set<MenuItem> active = new HashSet<>();
        for (MenuItemData data : snapshot.getItems()) {
            active.add(items.get(data.getSlot()).get(data.getPriority()));
        }
        holder.setActiveItems(active);

        if (options.refresh()) {
            holder.startRefreshTask();
        }

        if (isInMenu(holder.getViewer())) {
            closeMenu(plugin, holder.getViewer(), false);
        }

        viewer.openInventory(inventory);
        menuHolders.add(holder);

        if (updatePlaceholders) {
            holder.startUpdatePlaceholdersTask();
        }

        DeluxeMenusOpenMenuEvent openEvent = new DeluxeMenusOpenMenuEvent(viewer, holder);
        Bukkit.getPluginManager().callEvent(openEvent);
    }

    public void renderSnapshotToInventory(final @NotNull MenuHolder holder, final @NotNull MenuSnapshot snapshot, final @NotNull Inventory inventory) {
        // Clear inventory first
        inventory.clear();

        final Player viewer = holder.getViewer();
        for (MenuItemData template : snapshot.getItems()) {
            MenuItem item = items.get(template.getSlot()).get(template.getPriority());
            // Re-resolve dynamic fields against the current holder so placeholders
            // reflect the latest state, even if the snapshot was served from cache.
            final MenuItemData fresh = item.refreshDynamic(holder, template);
            ItemStack iStack = item.getItemStack(fresh, viewer);

            if (iStack != null) {
                iStack = plugin.getMenuItemMarker().mark(iStack);
                inventory.setItem(template.getSlot(), iStack);
            }
        }
    }

    public void refreshForAll() {
        menuHolders.stream().filter(menuHolder -> menuHolder.getMenuName().equalsIgnoreCase(options.name())).forEach(MenuHolder::refreshMenu);
    }

    public @NotNull Map<Integer, TreeMap<Integer, MenuItem>> getMenuItems() {
        return this.items;
    }

    public @NotNull Optional<String> getMenuCommandUsed(final @NotNull String command) {
        return this.options.commands().stream().filter(c -> c.equalsIgnoreCase(command)).findFirst();
    }

    public @NotNull MenuOptions options() {
        return this.options;
    }

    public @NotNull String path() {
        return this.path;
    }

    public int activeViewers() {
        return (int) menuHolders.stream().filter(holder -> holder.getMenuName().equalsIgnoreCase(options.name())).count();
    }

}
