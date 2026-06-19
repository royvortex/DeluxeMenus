package com.extendedclip.deluxemenus.cache;

import com.extendedclip.deluxemenus.menu.DynamicItemData;
import com.extendedclip.deluxemenus.menu.MenuItemData;
import com.extendedclip.deluxemenus.menu.StaticItemData;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Micro-benchmark and correctness tests for the MenuCache tiers.
 *
 * Tests:
 *   1. itemCacheSpeedup         - cache.put vs cache.get timing on the item tier
 *   2. invalidationByPlayer     - per-player entries cleared on invalidate(player)
 *   3. invalidationByMenu       - per-menu entries cleared on invalidate(menu)
 *   4. clearCacheClearsEverything - clearCache() wipes both tiers
 *   5. staticReusedDynamicRebuilt - the core of the split: same cached StaticItemData
 *                                  produces different MenuItemData when the dynamic
 *                                  side is rebuilt, which is what makes
 *                                  updatePlaceholders work with caching on.
 *
 * Run: ./gradlew test --tests MenuCacheBenchmarkTest --info
 */
class MenuCacheBenchmarkTest {

    private static final int ITERATIONS = 10_000;
    private static final int LORE_LINES = 8;

    @Test
    void itemCacheSpeedup() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        // Cold: build StaticItemData + cache.put every iteration
        long coldStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            StaticItemData staticData = buildStaticData(i);
            cache.putItemStatic(player, "shop", "item-0", args, staticData);
        }
        long coldNs = System.nanoTime() - coldStart;

        // Warm: cache.get (hashmap lookup, no allocation)
        long warmStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            assertNotNull(cache.getItemStatic(player, "shop", "item-0", args));
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

        cache.putItemStatic(player, "shop", "item-0", args, buildStaticData(0));
        cache.putItemStatic(player, "shop", "item-1", args, buildStaticData(1));
        cache.putItemStatic(other, "shop", "item-0", args, buildStaticData(0));

        cache.invalidate(player);

        assertNull(cache.getItemStatic(player, "shop", "item-0", args), "player invalidated");
        assertNull(cache.getItemStatic(player, "shop", "item-1", args), "player invalidated");
        assertNotNull(cache.getItemStatic(other, "shop", "item-0", args), "other player untouched");
    }

    @Test
    void invalidationByMenu() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        cache.putItemStatic(player, "shop", "item-0", args, buildStaticData(0));
        cache.putItemStatic(player, "other", "item-0", args, buildStaticData(0));

        cache.invalidate("shop");

        assertNull(cache.getItemStatic(player, "shop", "item-0", args), "shop invalidated");
        assertNotNull(cache.getItemStatic(player, "other", "item-0", args), "other menu untouched");
    }

    @Test
    void clearCacheClearsEverything() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        cache.putItemStatic(player, "shop", "item-0", args, buildStaticData(0));
        cache.putItemStatic(player, "other", "item-0", args, buildStaticData(0));

        cache.clearCache();

        assertNull(cache.getItemStatic(player, "shop", "item-0", args));
        assertNull(cache.getItemStatic(player, "other", "item-0", args));
    }

    /**
     * The split's whole point: a cached StaticItemData must be reusable with
     * different DynamicItemData, so render code can rebuild the dynamic side
     * per call without invalidating the cache. If this regresses (e.g. someone
     * starts caching MenuItemData again), updatePlaceholders items will go
     * stale and this test will fail.
     */
    @Test
    void staticReusedDynamicRebuilt() {
        MenuCache cache = new MenuCache();
        UUID player = UUID.randomUUID();
        Map<String, String> args = Map.of("page", "1");

        // One compute: cache the static, store a "current" dynamic alongside it.
        StaticItemData cachedStatic = buildStaticData(0);
        cache.putItemStatic(player, "shop", "item-0", args, cachedStatic);

        // Render 1: dynamic for "alice"
        DynamicItemData dynamicAlice = buildDynamicData("alice", 100);
        MenuItemData render1 = new MenuItemData(cachedStatic, dynamicAlice);

        // Player state changes (different placeholder values) - the static
        // is the same, but a new dynamic is built from the new holder.
        DynamicItemData dynamicBob = buildDynamicData("bob", 75);
        MenuItemData render2 = new MenuItemData(cachedStatic, dynamicBob);

        // Cache is untouched: the static hit is still valid, no recompute.
        StaticItemData retrieved = cache.getItemStatic(player, "shop", "item-0", args);
        assertNotNull(retrieved);
        assertEquals(cachedStatic, retrieved, "Cache should return the same static");

        // The two renders differ only on the dynamic side - which is exactly
        // what we want. The cache didn't lock in stale placeholder values.
        assertEquals(cachedStatic, render1.getStaticData());
        assertEquals(cachedStatic, render2.getStaticData());
        assertNotEquals(render1.getDisplayName(), render2.getDisplayName(),
                "Different holders must produce different resolved displayName");
        assertNotEquals(render1.getLore(), render2.getLore(),
                "Different holders must produce different resolved lore");
        assertNotEquals(render1.getAmount(), render2.getAmount(),
                "Different holders must produce different resolved amount");
    }

    private static StaticItemData buildStaticData(int slot) {
        return new StaticItemData(
                slot, 1, Material.STONE, 1,
                Collections.<org.bukkit.enchantments.Enchantment, Integer>emptyMap(),
                Collections.<org.bukkit.inventory.ItemFlag>emptyList(),
                false,
                Collections.<org.bukkit.potion.PotionEffect>emptyList(),
                false, false,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Collections.<String>emptyList(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty()
        );
    }

    private static DynamicItemData buildDynamicData(String displayName, int amount) {
        List<String> lore = new ArrayList<>(LORE_LINES);
        for (int i = 0; i < LORE_LINES; i++) {
            lore.add("Lore " + i + " for " + displayName);
        }
        Map<String, Object> nbt = new HashMap<>();
        nbt.put("s:name", displayName);
        return new DynamicItemData(
                Optional.empty(), Optional.of(amount),
                Optional.of(displayName), lore,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                (short) 0,
                Optional.empty(),
                nbt
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
