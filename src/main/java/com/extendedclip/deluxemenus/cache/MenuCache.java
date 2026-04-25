package com.extendedclip.deluxemenus.cache;

import com.extendedclip.deluxemenus.menu.MenuSnapshot;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A thread-safe cache for MenuSnapshot objects using Guava for LRU and TTL.
 */
public class MenuCache implements SimpleCache {

    private final Cache<CacheKey, MenuSnapshot> cache = CacheBuilder.newBuilder()
            .maximumSize(5000) // Cap at 5000 snapshots to prevent memory issues
            .expireAfterWrite(1, TimeUnit.MINUTES) // TTL of 1 minute
            .build();

    public void put(UUID playerUuid, String menuName, Map<String, String> args, MenuSnapshot snapshot) {
        cache.put(new CacheKey(playerUuid, menuName, args), snapshot);
    }

    public MenuSnapshot get(UUID playerUuid, String menuName, Map<String, String> args) {
        return cache.getIfPresent(new CacheKey(playerUuid, menuName, args));
    }

    public void invalidate(UUID playerUuid) {
        cache.asMap().keySet().removeIf(key -> key.playerUuid.equals(playerUuid));
    }

    public void invalidate(String menuName) {
        cache.asMap().keySet().removeIf(key -> key.menuName.equalsIgnoreCase(menuName));
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
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
