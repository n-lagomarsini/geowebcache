package org.geowebcache.storage.blobstore.memory.distributed;

import java.util.List;
import java.util.Set;

import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration.EvictionPolicy;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.CacheStatistics;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;

import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.SqlPredicate;

public class HazelcastCacheProvider implements CacheProvider {

    public static final String HAZELCAST_MAP_DEFINITION = "CacheProviderMap";
    
    public static final long MB_TO_BYTES = 1048576;

    private static final String HAZELCAST_NAME = "Hazelcast Cache";

    private final IMap<String, TileObject> map;

    private final boolean configured;

    private final long totalSize;

    public HazelcastCacheProvider(HazelcastLoader loader) {
        configured = loader.isConfigured();
        if (configured) {
            map = loader.getInstance().getMap(HAZELCAST_MAP_DEFINITION);
            totalSize = loader.getInstance().getConfig().getMapConfig(HAZELCAST_MAP_DEFINITION).getMaxSizeConfig().getSize() * MB_TO_BYTES;
        } else {
            map = null;
            totalSize = 0;
        }
    }

    @Override
    public TileObject getTileObj(TileObject obj) {
        if(configured){
            String key = GuavaCacheProvider.generateTileKey(obj);
            return map.get(key);
        }else{
            return null;
        }
    }

    @Override
    public void putTileObj(TileObject obj) {
        if(configured){
            String key = GuavaCacheProvider.generateTileKey(obj);
            map.put(key, obj);
        }
    }

    @Override
    public void removeTileObj(TileObject obj) {
        if(configured){
            String key = GuavaCacheProvider.generateTileKey(obj);
            map.remove(key);
        }
    }

    @Override
    public void removeLayer(String layername) {
//        if(configured){
//            EntryObject e = new PredicateBuilder().getEntryObject();
//            Predicate predicate = e.get("layer_name").equal(layername);
//            Set<TileObject> objs = (Set<TileObject>)map.values(predicate );
//            for(TileObject obj : objs){
//                String key = GuavaCacheProvider.generateTileKey(obj);
//                map.remove(key);
//            }
//        }
    }

    @Override
    public void clear() {
        if(configured){
            map.clear();
        }
    }

    @Override
    public void reset() {
        if(configured){
            map.clear();
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        if(configured){
            LocalMapStats localMapStats = map.getLocalMapStats();
            CacheStatistics stats = new HazelcastCacheStatistics(localMapStats, totalSize);
            return stats;
        }
        return new CacheStatistics();
    }

    @Override
    public void setConfiguration(CacheConfiguration configuration) {
    }

    @Override
    public void addUncachedLayer(String layername) {
    }

    @Override
    public void removeUncachedLayer(String layername) {
    }

    @Override
    public boolean containsUncachedLayer(String layername) {
        return false;
    }

    @Override
    public List<EvictionPolicy> getSupportedPolicies() {
        return null;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return configured;
    }

    @Override
    public String getName() {
        return HAZELCAST_NAME;
    }

    static class HazelcastCacheStatistics extends CacheStatistics{

        public HazelcastCacheStatistics(LocalMapStats localMapStats, long totalSize) {
            long hits = localMapStats.getHits();
            setHitCount(hits);
            long total = localMapStats.getOperationStats().getNumberOfGets();
            long miss = total - hits;
            setMissCount(miss);
            setTotalCount(total);
            double hitRate = ((int)(100 * ((1.0d * hits)/total)))/100d;
            double missRate = ((int)(100 * ((1.0d * miss)/total)))/100d;
            setHitRate(hitRate);
            setMissRate(missRate);            
            setTotalSize(totalSize);
            long actualSize = localMapStats.getOwnedEntryMemoryCost();
            setActualSize(actualSize);
            int currentMemoryOccupation = (int) (100L - (1L) * (100 * ((1.0d) * (totalSize - actualSize)) / totalSize));
            if (currentMemoryOccupation < 0) {
                currentMemoryOccupation = 0;
            }
            setCurrentMemoryOccupation(currentMemoryOccupation);
            setEvictionCount(localMapStats.getMarkedAsRemovedEntryCount());
        }
    }
}
