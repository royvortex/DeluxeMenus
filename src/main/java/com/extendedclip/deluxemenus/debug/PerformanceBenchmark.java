package com.extendedclip.deluxemenus.debug;

import com.extendedclip.deluxemenus.menu.MenuItemData;
import com.extendedclip.deluxemenus.menu.MenuSnapshot;
import com.extendedclip.deluxemenus.cache.MenuCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;

/**
 * A simple micro-benchmark to demonstrate the performance gain of the new caching system.
 */
public class PerformanceBenchmark {

    public static void runBenchmark() {
        MenuCache cache = new MenuCache();
        UUID playerUuid = UUID.randomUUID();
        String menuName = "LargeShop";
        Map<String, String> args = new HashMap<>();

        System.out.println("--- Performance Benchmark: MenuSnapshot Caching ---");

        // 1. Simulate a "Cold Open" (First time calculation)
        long startTime = System.nanoTime();
        MenuSnapshot coldSnapshot = simulateComputeSnapshot();
        cache.put(playerUuid, menuName, args, coldSnapshot);
        long endTime = System.nanoTime();
        double coldTimeMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Cold Open (Computation): %.4f ms%n", coldTimeMs);

        // 2. Simulate a "Warm Open" (Cache Hit)
        startTime = System.nanoTime();
        MenuSnapshot warmSnapshot = cache.get(playerUuid, menuName, args);
        endTime = System.nanoTime();
        double warmTimeMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Warm Open (Cache Hit):   %.4f ms%n", warmTimeMs);

        // 3. Difference
        double speedup = coldTimeMs / warmTimeMs;
        System.out.printf("Speedup Factor:          %.1fx faster%n", speedup);
        System.out.println("--------------------------------------------------");
    }

    private static MenuSnapshot simulateComputeSnapshot() {
        List<MenuItemData> items = new ArrayList<>();
        // Simulate a large 54-slot menu
        for (int i = 0; i < 54; i++) {
            // Simulate placeholder parsing delay
            simulatePlaceholderParsing();
            
            items.add(new MenuItemData(
                i,                          // slot
                1,                          // priority
                Material.STONE,             // material
                1,                          // amount
                java.util.Optional.of("Item " + i), // displayName
                java.util.List.of("Lore line 1", "Lore line 2", "Lore line 3"), // lore
                java.util.Map.of(),         // enchantments
                (short) 0,                  // damage
                java.util.Optional.empty(), // customModelData
                false,                      // unbreakable
                java.util.List.of(),        // itemFlags
                java.util.Optional.empty(), // rgbColor
                java.util.List.of(),        // potionEffects
                java.util.Optional.empty(), // rarity
                java.util.Optional.empty(), // hideTooltip
                java.util.Optional.empty(), // enchantmentGlintOverride
                java.util.Optional.empty(), // tooltipStyle
                java.util.Optional.empty(), // itemModel
                java.util.Optional.empty(), // trimMaterial
                java.util.Optional.empty(), // trimPattern
                java.util.Optional.empty(), // lightLevel
                java.util.Map.of(),         // nbtData
                java.util.Optional.empty(), // hookName
                java.util.Optional.empty(), // hookArgs
                java.util.Optional.empty(), // base64Data
                false,                      // isPlayerItem
                false                       // isWaterBottle
            ));
        }
        return new MenuSnapshot("Large Shop Title", 54, InventoryType.CHEST, items);
    }

    private static void simulatePlaceholderParsing() {
        // Simple spin-wait to simulate the cost of external PAPI calls / regex
        long start = System.nanoTime();
        while (System.nanoTime() - start < 50_000) { // 0.05ms per item
            // Do nothing
        }
    }
    
    public static void main(String[] args) {
        runBenchmark();
    }
}
