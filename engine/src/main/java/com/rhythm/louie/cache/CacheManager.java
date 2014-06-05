/*
 * CacheManager.java
 * 
 * Copyright (c) 2013 Rhythm & Hues Studios. All rights reserved.
 */
package com.rhythm.louie.cache;

import com.google.common.cache.CacheLoader;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilderSpec;

import org.slf4j.LoggerFactory;

/**
 *
 * @author cjohnson
 */
public class CacheManager {
    private static net.sf.ehcache.CacheManager defaultEhcacheManager;
    
    private static final Map<String,CacheManager> cacheManagers =
            Collections.synchronizedMap(new TreeMap<String,CacheManager>());
    
    private net.sf.ehcache.CacheManager ehCacheManager;
    private final Map<String,Cache<?,?>> caches;
    private final String name;
    
    private CacheManager(String name) {
        caches = new TreeMap<String,Cache<?,?>>();
        this.name = name;
    }
    
    public synchronized static void shutdown() {
        if (defaultEhcacheManager!=null) {
            defaultEhcacheManager.shutdown();
        }
        for (CacheManager manager : cacheManagers.values()) {
            if (manager.ehCacheManager!=null && manager.ehCacheManager!=defaultEhcacheManager) {
                manager.ehCacheManager.shutdown();
            }
        }
        cacheManagers.clear();
    }
    
    public static synchronized CacheManager createEhCacheManager(String name, URL ehConfigURL) {
        CacheManager cm = createCacheManager(name);
        cm.ehCacheManager =  new net.sf.ehcache.CacheManager(ehConfigURL);
        return cm;
    }
    
    public static synchronized CacheManager createCacheManager(String name) {
        if (cacheManagers.containsKey(name)) {
            LoggerFactory.getLogger(CacheManager.class)
                    .warn("CacheManager \"{}\" already exists!",name);
            return cacheManagers.get(name);
        }
        
        CacheManager cm = new CacheManager(name);
        cacheManagers.put(name, cm);
        return cm;
    }
    
    public static CacheManager createIfNeeded(String name) {
        CacheManager cm = cacheManagers.get(name);
        if (cm != null) {
            return cm;
        } else {
            return createCacheManager(name);
        }
    }
    
    
    public static CacheManager getCacheManager(String name) {
        return cacheManagers.get(name);
    }
    
    public static Collection<CacheManager> getCacheManagers() {
        return cacheManagers.values();
    }
    
    synchronized public static net.sf.ehcache.CacheManager getDefaultEhcacheManager() {
        if (defaultEhcacheManager == null) {
            defaultEhcacheManager = new net.sf.ehcache.CacheManager(CacheManager.class.getResource("ehcache.xml"));
        }
        return defaultEhcacheManager;
    }
    
    public String getName() {
        return name;
    }
    
    // Unsafe, must cleanup all managers
//    public void setEhcacheManager(net.sf.ehcache.CacheManager cacheManager) {
//        ehCacheManager = cacheManager;
//    }
//    public void loadEhcacheManager(URL configUrl) {
//        ehCacheManager = new net.sf.ehcache.CacheManager(configUrl);
//    }
//   
    /**
     * Creates a non-caching basic cache
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @return 
     */
    public <K, V> Cache<K, V> noCache(String cacheName) {
        synchronized (caches) {
            checkName(cacheName);

            GuavaBasicCache<K,V> cache = GuavaBasicCache.nonCaching(cacheName);
            caches.put(cacheName, cache);
            return cache;
        }
    }

    /**
     * Creates a non-expiring basic cache
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @return 
     */
    public <K, V> Cache<K, V> simpleCache(String cacheName) {
        synchronized (caches) {
            checkName(cacheName);

            GuavaBasicCache<K,V> cache = GuavaBasicCache.permanent(cacheName);
            caches.put(cacheName, cache);
            return cache;
        }
    }

    public <K, V> EhCache<K, V> createEHCache(String cacheName) {
        synchronized (caches) {
            if (ehCacheManager == null) {
                ehCacheManager = getDefaultEhcacheManager();
            }
            checkName(cacheName);

            EhCache<K, V> cache = new EhCache<K, V>(cacheName, ehCacheManager);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates a non-expiring cache
     * 
     * @param <V>
     * @param cacheName
     * @return 
     */
    public <V> SingletonCache<V> singletonCache(String cacheName) {
        synchronized (caches) {
            checkName(cacheName);

            SingletonCache<V> cache = SingletonCache.permanent(cacheName);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates a singleton cache from a CacheBuilderSpec
     * 
     * @param <V>
     * @param cacheName
     * @param spec
     * @return 
     */
    public <V> SingletonCache<V> singletonCache(String cacheName, CacheBuilderSpec spec) {
        synchronized (caches) {
            checkName(cacheName);

            SingletonCache<V> cache = SingletonCache.fromSpec(cacheName,spec);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates a singleton cache from a CacheBuilderSpec
     * 
     * @param <V>
     * @param cacheName
     * @param spec
     * @return 
     */
    public <V> SingletonCache<V> singletonCache(String cacheName, String spec) {
        synchronized (caches) {
            checkName(cacheName);

            SingletonCache<V> cache = SingletonCache.fromSpec(cacheName,spec);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates a basic cache from a CacheBuilderSpec
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @param spec
     * @return 
     */
    public <K, V> GuavaBasicCache<K,V> guavaCache(String cacheName, CacheBuilderSpec spec) {
        synchronized (caches) {
            checkName(cacheName);
            
            GuavaBasicCache<K,V> cache = GuavaBasicCache.fromSpec(cacheName, spec);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    /**
     * Creates a basic cache from a CacheBuilderSpec
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @param spec
     * @return 
     */
    public <K, V> GuavaBasicCache<K,V> guavaCache(String cacheName, String spec) {
        synchronized (caches) {
            checkName(cacheName);
            
            GuavaBasicCache<K,V> cache = GuavaBasicCache.fromSpec(cacheName, spec);
            caches.put(cacheName, cache);
            return cache;
        }
    }

    /**
     * Creates a cache populated by a CacheLoader based upon a CacheBuilderSpec
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @param spec
     * @param loader
     * @return 
     */
    public <K, V> GuavaLoadingCache<K,V> guavaLoadingCache(String cacheName, CacheBuilderSpec spec, CacheLoader<K,V> loader) {
        synchronized (caches) {
            checkName(cacheName);
            
            GuavaLoadingCache<K,V> cache = GuavaLoadingCache.fromSpec(name, spec, loader);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    /**
     * Creates a cache populated by a CacheLoader based upon a CacheBuilderSpec
     * 
     * @param <K>
     * @param <V>
     * @param cacheName
     * @param spec
     * @param loader
     * @return 
     */
    public <K, V> GuavaLoadingCache<K,V> guavaLoadingCache(String cacheName, String spec, CacheLoader<K,V> loader) {
        synchronized (caches) {
            checkName(cacheName);
            
            GuavaLoadingCache<K,V> cache = GuavaLoadingCache.fromSpec(name, spec, loader);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates a permanent storage cache
     * 
     * @param <V>
     * @param cacheName
     * @param supplier
     * @return 
     */
    public <V> SupplierCache<V> supplierCache(String cacheName, Supplier<V> supplier) {
        synchronized (caches) {
            checkName(cacheName);
            
            SupplierCache<V> cache = SupplierCache.permanent(name,supplier);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Creates an expiring cache.
     * if duration = 0 caching will be disabled
     * if duration < 0 the cache will never expire
     * 
     * @param <V>
     * @param cacheName
     * @param supplier
     * @param duration
     * @param timeUnit
     * @return 
     */
    public <V> SupplierCache<V> supplierCache(String cacheName, Supplier<V> supplier, long duration, TimeUnit timeUnit) {
        synchronized (caches) {
            checkName(cacheName);
            
            SupplierCache<V> cache = SupplierCache.expiring(name,supplier, duration, timeUnit);
            caches.put(cacheName, cache);
            return cache;
        }
    }
    
    /**
     * Registers a cache with the manager
     * 
     * @param cache
     * @return true if cache was properly registered, false if that name already exists
     */
    public boolean registerCache(Cache<?,?> cache) {
        synchronized (caches) {
            checkName(cache.getCacheName());
            caches.put(cache.getCacheName(), cache);
            return true;
        }
    }

    private void checkName(String cacheName) {
        if (caches.containsKey(cacheName)) {
            LoggerFactory.getLogger(CacheManager.class)
                    .warn("Cache \"{}:{}\" already exists!", name, cacheName);
        }
    }
    
    public Collection<Cache<?,?>> getCaches() {
        return caches.values();
    }
    
    public Cache<?,?> getCache(String cacheName) {
        return caches.get(cacheName);
    }
    
    public void clearAllCaches() throws Exception {
        for (Cache<?,?> cache : caches.values()) {
            cache.clear();
        }
    }
}
