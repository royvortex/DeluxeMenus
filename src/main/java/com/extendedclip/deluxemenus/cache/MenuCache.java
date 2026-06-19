package com.extendedclip.deluxemenus.cache;

import com.extendedclip.deluxemenus.menu.MenuSnapshot;
import com.extendedclip.deluxemenus.menu.StaticItemData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A thread-safe cache for MenuSnapshot and StaticItemData using Guava for LRU and TTL.
 *
 * The item tier stores only the static, config-derived part of an item. The
 * dynamic part (placeholders, etc.) is rebuilt on every render, so it never
 * goes in the cache. This is what keeps {@code updatePlaceholders: true}
 * items working correctly even with caching enabled.
 */
public class MenuCache implements SimpleCache {

    private final Cache<CacheKey, MenuSnapshot> cache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    private final Cache<ItemCacheKey, StaticItemData> itemCache = CacheBuilder.newBuilder()
            .maximumSize(20000) // Individual items take less memory, can cache more
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    public void putItemStatic(UUID playerUuid, String menuName, String itemKey, Map<String, String> args, StaticItemData data) {
        itemCache.put(new ItemCacheKey(playerUuid, menuName, itemKey, args), data);
    }

    public StaticItemData getItemStatic(UUID playerUuid, String menuName, String itemKey, Map<String, String> args) {
        return itemCache.getIfPresent(new ItemCacheKey(playerUuid, menuName, itemKey, args));
    }

    public void put(UUID playerUuid, String menuName, Map<String, String> args, MenuSnapshot snapshot) {
        cache.put(new CacheKey(playerUuid, menuName, args), snapshot);
    }

    public MenuSnapshot get(UUID playerUuid, String menuName, Map<String, String> args) {
        return cache.getIfPresent(new CacheKey(playerUuid, menuName, args));
    }

    public void invalidate(UUID playerUuid) {
        cache.asMap().keySet().removeIf(key -> key.playerUuid.equals(playerUuid));
        itemCache.asMap().keySet().removeIf(key -> key.playerUuid.equals(playerUuid));
    }

    public void invalidate(String menuName) {
        cache.asMap().keySet().removeIf(key -> key.menuName.equalsIgnoreCase(menuName));
        itemCache.asMap().keySet().removeIf(key -> key.menuName.equalsIgnoreCase(menuName));
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
        itemCache.invalidateAll();
    }

    private static class ItemCacheKey {
        private final UUID playerUuid;
        private final String menuName;
        private final String itemKey;
        private final Map<String, String> args;

        public ItemCacheKey(UUID playerUuid, String menuName, String itemKey, Map<String, String> args) {
            this.playerUuid = playerUuid;
            this.menuName = menuName;
            this.itemKey = itemKey;
            this.args = args != null ? Map.copyOf(args) : Map.of();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemCacheKey that = (ItemCacheKey) o;
            return Objects.equals(playerUuid, that.playerUuid) &&
                    Objects.equals(menuName.toLowerCase(), that.menuName.toLowerCase()) &&
                    Objects.equals(itemKey.toLowerCase(), that.itemKey.toLowerCase()) &&
                    Objects.equals(args, that.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerUuid, menuName.toLowerCase(), itemKey.toLowerCase(), args);
        }
    }

    private static class CacheKey {
        private final UUID playerUuid;
        private final String menuName;
        private final Map<String, String> args;

        public CacheKey(UUID playerUuid, String menuName, Map<String, String> args) {
            this.playerUuid = playerUuid;
            this.menuName = menuName;
            this.args = args != null ? Map.copyOf(args) : Map.of();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(playerUuid, cacheKey.playerUuid) &&
                    Objects.equals(menuName.toLowerCase(), cacheKey.menuName.toLowerCase()) &&
                    Objects.equals(args, cacheKey.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerUuid, menuName.toLowerCase(), args);
        }
    }
}
