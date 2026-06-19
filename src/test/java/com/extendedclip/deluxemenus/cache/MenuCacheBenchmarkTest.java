package com.extendedclip.deluxemenus.cache;

import com.extendedclip.deluxemenus.menu.MenuItemData;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Micro-benchmark for the MenuCache on the data path the cache stores.
 *
 * Tests:
 *   1. itemCacheSpeedup      - MenuItemData construction (cold) vs cache.get (warm)
 *   2. invalidationByPlayer  - cache cleared for that player
 *   3. invalidationByMenu    - cache cleared for that menu
 *
 * Run: ./gradlew test --tests MenuCacheBenchmarkTest --info
 *
 * What this measures: the allocation cost the cache skips (building the
 * MenuItemData POJO) vs the cost the cache does (the hashmap lookup).
 *
 * What this does NOT measure: MenuItem.computeData also resolves PAPI
 * placeholders, parses hooks, and walks NBT per item. All of that is also
 * cached, so the real production speedup is higher than what this test shows.
 *
 * The snapshot cache (MenuCache.cache) and the snapshot benchmark are not
 * tested here because MenuSnapshot requires InventoryType, whose static
 * initializer calls into Bukkit's MenuType registry, which needs a running
 * server. The cache code path is identical for both tiers (Guava Cache with
 * the same key shape), so testing the item tier exercises the same lookup
 * logic.
 */
class MenuCacheBenchmarkTest {

    private static final int ITERATIONS = 10_000;
    private static final int LORE_LINES = 8;

    @Test
    void itemCacheSpeedup() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        // Cold: build MenuItemData + cache.put every iteration
        long coldStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            MenuItemData data = buildItemData(i);
            cache.putItem(player, "shop", "item-0", args, data);
        }
        long coldNs = System.nanoTime() - coldStart;

        // Warm: cache.get (hashmap lookup, no allocation)
        long warmStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            assertNotNull(cache.getItem(player, "shop", "item-0", args));
        }
        long warmNs = System.nanoTime() - warmStart;

        report("Item cache (1 item x " + ITERATIONS + "x)", coldNs, warmNs);
        assertTrue(coldNs > warmNs, "Cache hit should be faster than rebuild");
    }

    @Test
    void invalidationByPlayer() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        cache.putItem(player, "shop", "item-0", args, buildItemData(0));
        cache.putItem(player, "shop", "item-1", args, buildItemData(1));
        cache.putItem(other, "shop", "item-0", args, buildItemData(0));

        cache.invalidate(player);

        assertNull(cache.getItem(player, "shop", "item-0", args), "player invalidated");
        assertNull(cache.getItem(player, "shop", "item-1", args), "player invalidated");
        assertNotNull(cache.getItem(other, "shop", "item-0", args), "other player untouched");
    }

    @Test
    void invalidationByMenu() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        cache.putItem(player, "shop", "item-0", args, buildItemData(0));
        cache.putItem(player, "other", "item-0", args, buildItemData(0));

        cache.invalidate("shop");

        assertNull(cache.getItem(player, "shop", "item-0", args), "shop invalidated");
        assertNotNull(cache.getItem(player, "other", "item-0", args), "other menu untouched");
    }

    @Test
    void clearCacheClearsEverything() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        cache.putItem(player, "shop", "item-0", args, buildItemData(0));
        cache.putItem(player, "other", "item-0", args, buildItemData(0));

        cache.clearCache();

        assertNull(cache.getItem(player, "shop", "item-0", args));
        assertNull(cache.getItem(player, "other", "item-0", args));
    }

    private static MenuItemData buildItemData(int slot) {
        List<String> lore = new ArrayList<>(LORE_LINES);
        for (int i = 0; i < LORE_LINES; i++) {
            lore.add("Lore line " + i + " with placeholder text %player_name% %player_health%");
        }
        Map<String, Object> nbt = new HashMap<>();
        nbt.put("s:custom_key", "custom_value_" + slot);
        nbt.put("i:custom_int", slot);
        return new MenuItemData(
                slot, 1, Material.STONE, 1,
                Optional.of("Item " + slot),
                lore, Map.of(), (short) 0,
                Optional.empty(), false, List.of(),
                Optional.empty(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), nbt,
                Optional.empty(), Optional.empty(), Optional.empty(),
                false, false
        );
    }

    private static void report(String label, long coldNs, long warmNs) {
        double coldMs = coldNs / 1_000_000.0;
        double warmMs = warmNs / 1_000_000.0;
        double speedup = coldMs / warmMs;
        System.out.printf("--- %s ---%n", label);
        System.out.printf("  cold: %8.2f ms%n", coldMs);
        System.out.printf("  warm: %8.3f ms%n", warmMs);
        System.out.printf("  speedup: %.1fx%n", speedup);
    }
}
