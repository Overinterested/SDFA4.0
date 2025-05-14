package edu.sysu.pmglab.easytools.container;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Wenjie Peng
 * @create 2024-10-10 20:14
 * @description
 */
public class GlobalCache {
    private static final GlobalCache instance = new GlobalCache();
    private ConcurrentHashMap<Object, Object> cache = new ConcurrentHashMap<>();

    public Object get(Object k) {
        return cache.get(k);
    }

    public void putIfAbsent(Object k, Object v) {
        cache.putIfAbsent(k, v);
    }

    public static GlobalCache getInstance() {
        return instance;
    }

    public void put(Object k, Object v) {
        cache.put(k, v);
    }
}
