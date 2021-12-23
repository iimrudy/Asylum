package eu.asylum.common.helpers;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 20/11/2021 AsylumCache with ttl & concurrent hashmaps
public abstract class AsylumCache<K, V> {

    private final long timeToLive; // Cache KeyValue

    private final Map<K, V> cache = new ConcurrentHashMap<K, V>();

    public AsylumCache(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public abstract boolean canDelete(K key, V value);


}
