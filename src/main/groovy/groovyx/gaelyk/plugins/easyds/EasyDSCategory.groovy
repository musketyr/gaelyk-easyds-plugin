package groovyx.gaelyk.plugins.easyds

import java.util.Map;
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

import groovy.lang.GString;
import groovyx.gaelyk.GaelykCategory;

class EasyDSCategory {
	
	static int countAll(String kind){
		GaelykCategory.execute(ds) { select count from kind }
	}
	
	static int countBy(String kind, Map<String,Object> props){
		Query query = new Query(kind)
		for(Entry<String, Object> entry in props){
			query.addFilter(entry.key, FilterOperator.EQUAL, entry.value)
		}
		PreparedQuery pq = ds.prepare(query)
		pq.countEntities(FetchOptions.Builder.withDefaults())
	}
	
	static List<Entity> fetchAllBy(String kind, Map<String,Object> props, Map<String,Object> config = [:]){
		Query query = new Query(kind)
		for(Entry<String, Object> entry in props){
			query.addFilter(entry.key, FilterOperator.EQUAL, entry.value)
		}
		if(config.sort){
			switch(config.sort){
				case ['asc', SortDirection.ASCENDING]	: query.addSort(config.sort, SortDirection.ASCENDING); break
				case ['desc', SortDirection.DESCENDING]	: query.addSort(config.sort, SortDirection.DESCENDING); break
				default: throw new IllegalArgumentException("Sort must be either 'asc' or 'desc' or constant of SortDirection")
			}
		}
		PreparedQuery pq = ds.prepare(query)
		FetchOptions options = FetchOptions.Builder.withDefaults()
		if(config.max){
			options = options.limit(config.max as Integer)
		}
		if(config.offset){
			options = options.offset(config.offset as Integer)
		}
		pq.asList(options)
	}
	
	static Entity fetchBy(String kind, Map<String,Object> props, Map<String,Object> config = [:]){
		config.max = 1
		List<Entity> all = fetchAllBy(kind, props, config)
		
		if(all){
			all[0]
		} else {
			null
		}
	}
	
	static boolean exists(Map<String, Object> key){
		try{ 
			ds.get(mapToKey(key))
			return true
		} catch (EntityNotFoundException e){
			return false
		}
	}
	
	static boolean existsAll(Map<String, Object> key){
		try{
			ds.get(mapToKeys(key))
			return true
		} catch (EntityNotFoundException e){
			return false
		}
	}
	
	static void delete(Map<String, Object> key){
		ds.delete(mapToKey(key))
	}
	
	static void deleteAll(Map<String, Object> key){
		ds.delete(mapToKeys(key))
	}
	
	static Entity fetch(Map<String, Object> key){
		ds.get(mapToKey(key))
	}
	
	static Set<Entity> fetchAll(Map<String, Object> key){
		ds.get(mapToKeys(key)).values()
	}
	
	static List<Entity> list(String entity, Map<String, Object> config = [:]){
		fetchAllBy(entity, [:], config)
	}

	static Entity update(Map<String, Object> key, Map<String, Object> values){
		update fetch(key), values
	}
	
	static Entity update(Entity entity, Map<String, Object> values){
		for(Entry<String, Object> entry in values.entrySet()){
				entity.setProperty(entry.key, entry.value)
		}
		GaelykCategory.save(entity)
		entity
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
		if(dirty) { GaelykCategory.save(entity) }
		entity
	}
	
	static Map<String, String> validate(Entity entity, Map<String, Closure> validators){
		validate(entity.properties, validators)
	}
	
	static Map<String, String> validate(Map<String, Object> props, Map<String, Closure> validators){
		Map<String, String> errors = [:]
		for(Entry<String, Closure> entry in validators){
			if(!props.containsKey(entry.key)){
				continue
			}
			def result = executeValidationClosure(props[entry.key], props, entry.value)
			if(result){
				errors[entry.key] = result
			}
		}
		errors
	}
	
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

	private static Iterable<Key> mapToKeys(Map keys) {
		assert keys && keys.size() == 1
		Entry<String, Object>  entry = keys.entrySet().asList().first()
		assert entry.value instanceof Iterable
		entry.value.collect{ KeyFactory.createKey(entry.key, it as Long) }
	}

	private static DatastoreService getDs(){
		DatastoreServiceFactory.datastoreService
	}
}
