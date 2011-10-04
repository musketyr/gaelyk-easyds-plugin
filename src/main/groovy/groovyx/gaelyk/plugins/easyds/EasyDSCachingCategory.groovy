package groovyx.gaelyk.plugins.easyds

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import groovy.lang.GString;
import groovyx.gaelyk.GaelykCategory;

class EasyDSCachingCategory {
	
	static int countAll(String kind){
		cacheInternal(getCountAllKey(kind)){
			EasyDSCategory.countAll(kind)
		}
	}
	
	static int countBy(String kind, Map<String,Object> props){
		cacheInternal(getCountByKey(kind, props)){
			EasyDSCategory.countBy(kind, props)
		}
	}
	
	static List<Entity> fetchAllBy(String kind, Map<String,Object> props, Map<String,Object> config = [:]){
		cacheInternal(getFetchAllByKey(kind, props,config)){
			EasyDSCategory.fetchAllBy(kind, props, config)
		}
	}
	
	static Entity fetchBy(String kind, Map<String,Object> props, Map<String,Object> config = [:]){
		cacheInternal(getFetchByKey(kind, props, config)){
			EasyDSCategory.fetchBy(kind, props, config)
		}
	}
	
	static boolean exists(Map<String, Object> key){
		cacheInternal(getExistsKey(mapToKind(key), mapToValue(key))){
			EasyDSCategory.exists(key)
		}
	}
	
	static boolean existAll(Map<String, Object> key){
		cacheInternal(getExistAllKey(mapToKind(key), mapToValue(key))){
			EasyDSCategory.existAll(key)
		}
	}
	
	static void delete(Map<String, Object> key){
		EasyDSCategory.delete(key)
		invalidateCache(mapToKind(key))
	}
	
	static void deleteAll(Map<String, Object> key){
		EasyDSCategory.deleteAll(key)
		invalidateCache(mapToKind(key))
	}
	
	static Entity fetch(Map<String, Object> key){
		cacheInternal(getFetchKey(mapToKind(key), mapToValue(key))) { 
			EasyDSCategory.fetch(key)
		}
	}
	
	static Set<Entity> fetchAll(Map<String, Object> key){
		cacheInternal(getFetchAllKey(mapToKind(key), mapToValue(key))) {
			EasyDSCategory.fetchAll(key)
		}
	}
	
	static Entity safeFetch(Map<String, Object> key){
		try{
			return fetch(key)
		} catch (EntityNotFoundException e){
			return null
		}
	}
	
	static void safeDelete(Map<String, Object> key){
		try{
			delete(key)
		} catch (e){
			// noop
		}
	}
	static void safeDeleteAll(Map<String, Object> key){
		try {
			deleteAll(mapToKeys(key))
		} catch (e) {
			// noop
		}
	}
	
	static Set<Entity> safeFetchAll(Map<String, Object> key){
		mapToKeys(key).collect{ safeFetch([(mapToKind(key)):it]) }.grep()
	}
	
	static List<Entity> list(String entity, Map<String, Object> config = [:]){
		fetchAllBy(entity, [:], config)
	}

	static Entity update(Map<String, Object> key, Map<String, Object> values){
		update fetch(key), values
	}
	
	static Entity update(Entity entity, Map<String, Object> values){
		invalidateCache(entity.kind)
		Entity updated = EasyDSCategory.update(entity, values)
		cacheInternal(getFetchKey(updated.kind, updated.key.id)) { updated }
	}
	
	static Entity create(String entityName, Map<String, Object> values){
		update new Entity(entityName), values
	}
	
	
	static Entity updateIfNotSet(Map<String, Object> key, Map<String, Object> values){
		updateIfNotSet fetch(key), values
	}
	
	static Entity updateIfNotSet(Entity entity, Map<String, Object> values){
		boolean dirty = false
		for(Entry<String, Object> entry in values.entrySet()){
			if(!entity.hasProperty(entry.key) || !entity.getProperty(entry.key)){
				entity.setProperty(entry.key, entry.value)
				dirty = true
			}
		}
		if(dirty) { 
			GaelykCategory.save(entity)
			invalidateCache(entity.kind)
		}
		cacheInternal(getFetchKey(entity.kind, entity.key.id)) { entity }
	}
	
	static Map<String, String> validate(Entity entity, Map<String, Closure> validators){
		EasyDSCategory.validate(entity, validators)
	}
	
	static Map<String, String> validate(Map<String, Object> props, Map<String, Closure> validators){
		EasyDSCategory.validate(props, validators)
	}
	
	static void invalidateCache(String kind){
		for(String key in memcacheKeyRegistry.findAll{ String it -> it.startsWith(getKindPrefix(kind)) }){
			memcache.delete(key)
		}
	}
	
	static cache(String kind, String key, Closure closure){
		cacheInternal(getCustomKey(kind, key), closure)
	}
	
	
	// GaelykCatgeory overrides
	static save(Entity entity){
		update entity, [:]
	}
	
	static delete(Key key){
		delete([(key.kind): key.id])
	}
	
	static delete(Entity entity){
		delete entity.key
	}
	
	static asyncSave(Entity entity){
		invalidateCache(entity.kind)
		GaelykCategory.asyncSave(entity)
	}
	
	static asyncDelete(Key key){
		invalidateCache(key.kind)
		GaelykCategory.asyncDelete(key)
	}
	
	static asyncDelete(Entity entity){
		invalidateCache(entity.kind)
		GaelykCategory.asyncDelete(entity)
	}
	
	
	private static String MEMCACHE_KEY_REGISTRY_KEY = "easyds::memcacheKeyRegistry"
	
	private static executeValidationClosure(value, Map<String, Object> props, Closure closure){
		switch(closure.parameterTypes.size()){
				case 0: return closure.call()
				case 1: return closure.call(value)
				case 2: return closure.call(value, props)
				default: throw new IllegalArgumentException("Validation closure ${closure} accepts to many parameters!")
		}
	}
	
	private static Key mapToKey(Map key) {
		assert key && key.size() == 1
		Entry<String, Object>  entry = key.entrySet().asList().first()
		KeyFactory.createKey(entry.key, entry.value as Long)
	}
	
	private static String mapToKind(Map key) {
		assert key && key.size() == 1
		Entry<String, Object>  entry = key.entrySet().asList().first()
		entry.key
	}
	
	private static String mapToValue(Map key) {
		assert key && key.size() == 1
		Entry<String, Object>  entry = key.entrySet().asList().first()
		entry.value
	}

	private static Iterable<Key> mapToKeys(Map keys) {
		assert keys && keys.size() == 1
		Entry<String, Object>  entry = keys.entrySet().asList().first()
		assert entry.value instanceof Iterable
		entry.value.collect{ KeyFactory.createKey(entry.key, it as Long) }
	}

	private static DatastoreService getDs(){
		DatastoreServiceFactory.datastoreService
	}
	
	private static MemcacheService getMemcache(){
		MemcacheServiceFactory.memcacheService
	}
	
	private static cacheInternal(String key, Closure closure){
		if(memcache.contains(key)){
			return memcache.get(key)
		}
		def result =  closure.call()
		memcache.put(key, result)
		registerMemcacheKey(key)
		result
	}
	
	private static registerMemcacheKey(String key){
		def registry = memcacheKeyRegistry
		registry << key
		memcache.put(MEMCACHE_KEY_REGISTRY_KEY, registry)
	}
	
	private static getMemcacheKeyRegistry(){
		def registry = memcache.get(MEMCACHE_KEY_REGISTRY_KEY)
		if(registry){
			return new LinkedHashSet<String>(registry)
		}
		new LinkedHashSet<String>()
	}
	
	private static String getKindPrefix(String kind){
		"easyds:$kind"
	}
	
	private static String getFetchByKey(String kind, Map<String, Object> props, Map<String, Object> config = [:]){
		"${getKindPrefix(kind)}:plugin:fetchBy:${config.toMapString()}:${props.toMapString()}"
	}
	
	private static String getFetchAllByKey(String kind, Map<String, Object> props, Map<String, Object> config = [:]){
		"${getKindPrefix(kind)}:plugin:fetchAllBy:${config.toMapString()}:${props.toMapString()}"
	}
	
	private static String getExistsKey(String kind, id){
		"${getKindPrefix(kind)}:plugin:exists:$id"
	}
	
	private static String getExistAllKey(String kind, ids){
		"${getKindPrefix(kind)}:plugin:existAll:$ids"
	}
	
	private static String getFetchKey(String kind, id){
		"${getKindPrefix(kind)}:plugin:fetch:$id"
	}
	
	private static String getFetchAllKey(String kind, ids){
		"${getKindPrefix(kind)}:plugin:fetchAll:$ids"
	}
	
	private static String getCountAllKey(String kind){
		"${getKindPrefix(kind)}:plugin:countAll"
	}
	
	private static String getCustomKey(String kind, String key){
		"${getKindPrefix(kind)}:custom:$key"
	}
	
	private static String getListKey(String kind, Map<String,Object> config = [:]){
		getFetchAllByKey(kind, [:], config)
	}
	
	private static String getCountByKey(String kind, Map<String,Object> props){
		"${getKindPrefix(kind)}:plugin:countBy:${props.toMapString()}"
	}

	
	
}
