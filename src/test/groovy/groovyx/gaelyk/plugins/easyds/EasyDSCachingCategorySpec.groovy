package groovyx.gaelyk.plugins.easyds

import java.util.Map.Entry

import spock.lang.Specification

import com.google.appengine.api.datastore.DatastoreService
import com.google.appengine.api.datastore.DatastoreServiceFactory
import com.google.appengine.api.datastore.Entity
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig
import com.google.appengine.tools.development.testing.LocalServiceTestHelper

public class EasyDSCachingCategorySpec extends Specification {

	
	def 'Count all entities'(){
		String key = EasyDSCachingCategory.getCountAllKey(kind)
		
		expect:
		EasyDSCachingCategory.countAll(kind) == count
		memcache.contains(key)
		memcache.get(key) == count
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
		
		where:
		kind			| count
		'book'			| 3
		"movi${'e'}"	| 2
		'music'			| 0
	}
	
	def 'Count by property'(){
		String key = EasyDSCachingCategory.getCountByKey(kind, props)
		
		expect:
		EasyDSCachingCategory.countBy(kind, props) == count
		memcache.contains(key)
		memcache.get(key) == count
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
		
		where:
		kind 	| count | props
		'book'	| 2		| [owned: false]
		'movie'	| 1		| [owned: true]
		'book'	| 1		| [author: 'Dan Brown']
	}
	
	def 'Delete entity specified by map - id is number'(){
		expect:
		EasyDSCachingCategory.countAll('book') == 3
		
		when:
		EasyDSCachingCategory.delete(book: itKey.id)
		
		then:
		EasyDSCachingCategory.countAll('book') == 2
	}
	
	def 'Delete entity specified by map - id is list of numbers'(){
		expect:
		EasyDSCachingCategory.countAll('book') == 3
		
		when:
		EasyDSCachingCategory.deleteAll(book: [itKey.id, daVinciKey.id])
		
		then:
		EasyDSCachingCategory.countAll('book') == 1
	}
	
	def 'Entity exists'(){
		expect:
		EasyDSCachingCategory.exists(book: itKey.id)
		EasyDSCachingCategory.existAll(book: [itKey.id, daVinciKey.id])
		!EasyDSCachingCategory.exists(book: 123456)
		
		when:
		String existsKey = EasyDSCachingCategory.getExistsKey("book", itKey.id)
		String existAllKey = EasyDSCachingCategory.getExistAllKey("book", [itKey.id, daVinciKey.id])
		String missing = EasyDSCachingCategory.getExistsKey("book", 123456)
		
		then:
		memcache.contains(existsKey)
		memcache.get(existsKey)
		
		memcache.contains(existAllKey)
		memcache.get(existAllKey)
		
		memcache.contains(missing)
		!memcache.get(missing)
		
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 3
		
		existsKey in EasyDSCachingCategory.memcacheKeyRegistry
		existAllKey in EasyDSCachingCategory.memcacheKeyRegistry
		missing in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch all entities by'(){
		String key = EasyDSCachingCategory.getFetchAllByKey(kind, props)
		
		expect:
		EasyDSCachingCategory.fetchAllBy(kind, props).size() == count
		
		memcache.contains(key)
		memcache.get(key).size() == count
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
		
		where:
		kind 	| count | props
		'book'	| 2		| [owned: false]
		'movie'	| 1		| [owned: true]
		'book'	| 1		| [author: 'Dan Brown']
		'book'	| 0		| [author: 'Karel Čapek']
	}
	
	def 'Fetch all offset'(){
		List<Entity> books = EasyDSCachingCategory.fetchAllBy('book', [owned: false], [offset: 1])
		String key = EasyDSCachingCategory.getFetchAllByKey('book', [owned: false], [offset: 1])
		
		expect:
		books.size() == 1
		books[0].getProperty('published') == 1982
		
		memcache.contains(key)
		memcache.get(key).size() == 1
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch all max'(){
		List<Entity> books = EasyDSCachingCategory.fetchAllBy('book', [owned: false], [max: 1])
		String key = EasyDSCachingCategory.getFetchAllByKey('book', [owned: false], [max: 1])
		
		expect:
		books.size() == 1
		books[0].getProperty('published') == 2003
		
		memcache.contains(key)
		memcache.get(key).size() == 1
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch all sorted'(){
		List<Entity> books = EasyDSCachingCategory.fetchAllBy('book', [owned: false], [sort: "published"])
		String key = EasyDSCachingCategory.getFetchAllByKey('book', [owned: false], [sort: "published"])
		
		expect:
		books.size() == 2
		books[0].getProperty('published') == 1982
		books[1].getProperty('published') == 2003
		
		memcache.contains(key)
		memcache.get(key).size() == 2
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch all sorted desc'(){
		List<Entity> books = EasyDSCachingCategory.fetchAllBy('book', [owned: false], [sort: "published", order: 'desc'])
		String key = EasyDSCachingCategory.getFetchAllByKey('book', [owned: false], [sort: "published", order: 'desc'])
		
		expect:
		books.size() == 2
		books[0].getProperty('published') == 2003
		books[1].getProperty('published') == 1982
		
		memcache.contains(key)
		memcache.get(key).size() == 2
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch entity by'(){
		String ownedKey = EasyDSCachingCategory.getFetchByKey('movie', [owned: true])
		String notOwnedKey = EasyDSCachingCategory.getFetchByKey('book', [owned: false])
		String capekKey = EasyDSCachingCategory.getFetchByKey('book', [author: 'Karek Čapek'])
		
		expect:
		EasyDSCachingCategory.fetchBy('movie', [owned: true]).getProperty('released')	== 1994
		EasyDSCachingCategory.fetchBy('book', [owned: false]).getProperty('published')	== 2003
		EasyDSCachingCategory.fetchBy('book', [author: 'Karek Čapek'])					== null
		
		memcache.contains(ownedKey)
		memcache.get(ownedKey).getProperty('released')									== 1994
		
		memcache.contains(notOwnedKey)
		memcache.get(notOwnedKey).getProperty('published')								== 2003
		
		memcache.contains(capekKey)
		!memcache.get(capekKey)
		
		EasyDSCachingCategory.memcacheKeyRegistry.findAll{ it.contains("fetchBy")}.size() == 3
		
		ownedKey in EasyDSCachingCategory.memcacheKeyRegistry
		notOwnedKey in EasyDSCachingCategory.memcacheKeyRegistry
		capekKey in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	def 'Fetch by id'(){
		String fetchKey = EasyDSCachingCategory.getFetchKey("book", itKey.id)
		String fetchAllKey = EasyDSCachingCategory.getFetchAllKey("book", [itKey.id, daVinciKey.id])
		
		
		expect:
		EasyDSCachingCategory.fetch(book: itKey.id).getProperty('published') 	== 1987
		EasyDSCachingCategory.fetchAll(book: [itKey.id, daVinciKey.id]).size()	== 2
		
		memcache.contains(fetchKey)
		memcache.get(fetchKey).getProperty('published')	== 1987
		
		memcache.contains(fetchAllKey)
		memcache.get(fetchAllKey).size() == 2
		

		EasyDSCachingCategory.memcacheKeyRegistry.size() == 2
		
		fetchKey in EasyDSCachingCategory.memcacheKeyRegistry
		fetchAllKey in EasyDSCachingCategory.memcacheKeyRegistry
	}
	
	
	def 'List all entities'(){
		String key = EasyDSCachingCategory.getListKey(kind)
		
		expect:
		EasyDSCachingCategory.list(kind).size() == count
		
		memcache.contains(key)
		memcache.get(key).size() == count
		EasyDSCachingCategory.memcacheKeyRegistry.size() == 1
		key in EasyDSCachingCategory.memcacheKeyRegistry
		
		where:
		kind			| count
		'book'			| 3
		"movi${'e'}"	| 2
		'music'			| 0
	}
	
	def 'Create entity'(){
		expect:
		EasyDSCachingCategory.countAll('book') == 3
		
		when:
		Entity entity = EasyDSCachingCategory.create('book', [author: 'Stephen King', title: 'Cujo', published: 1993, owned: false])
		then:
		EasyDSCachingCategory.countAll('book') == 4
		
		when:
		String key = EasyDSCachingCategory.getFetchKey('book', entity.key.id)
		
		then:
		memcache.contains(key)
		memcache.get(key).getProperty('title')	== 'Cujo'
		
		
	}
	
	def 'Update map entity'(){
		EasyDSCachingCategory.update([book: itKey.id], [published: 1990])
		String key = EasyDSCachingCategory.getFetchKey('book', itKey.id)
		
		expect:
		memcache.contains(key)
		memcache.get(key).getProperty('published')	== 1990
		
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('published') == 1990
	}
	
	def 'Update entity'(){
		EasyDSCachingCategory.update(EasyDSCachingCategory.fetch([book: itKey.id]), [published: 1990])
		String key = EasyDSCachingCategory.getFetchKey('book', itKey.id)
		
		expect:
		memcache.contains(key)
		memcache.get(key).getProperty('published')	== 1990
		
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('published') == 1990
	}
	
	def 'Update map entity if not value set'(){
		EasyDSCachingCategory.updateIfNotSet([book: itKey.id], [published: 1990, haluz: true])
		String key = EasyDSCachingCategory.getFetchKey('book', itKey.id)
		
		expect:
		memcache.contains(key)
		memcache.get(key).getProperty('published')	== 1987
		memcache.get(key).getProperty('haluz') 		== true
		
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('published') 	== 1987
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('haluz') 		== true
	}
	
	def 'Update entity if not value set'(){
		EasyDSCachingCategory.updateIfNotSet(EasyDSCachingCategory.fetch([book: itKey.id]), [published: 1990, haluz: true])
		String key = EasyDSCachingCategory.getFetchKey('book', itKey.id)
		
		expect:
		memcache.contains(key)
		memcache.get(key).getProperty('published')	== 1987
		memcache.get(key).getProperty('haluz') 		== true
		
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('published') == 1987
		EasyDSCachingCategory.fetch([book: itKey.id]).getProperty('haluz') == true
	}
	
	def 'Validate entity'(){
		def trueKing = { value, entity ->
				if(value == 'It' && entity.author != 'Stephen King'){
					return 'The only true author of It is Stephen King!'
				}
		}
		
		def authorShort = {
				if(it.size() < 4){
					return 'Author too short'
				}
		}
		
		def authorLong = {
			if(it.size() > 20){
				return 'Author too long'
			}
		}
		
		expect:
		
		!trueKing('It', [author: 'Stephen King', title: 'It'])
		trueKing('It', [author: 'Poe', title: 'It']) == 'The only true author of It is Stephen King!'
		!authorShort('King')
		authorShort('Poe') == 'Author too short'
		
		EasyDSCachingCategory.validate(EasyDSCachingCategory.create('book', props), [title: trueKing, author: authorShort]) == errors
		
		where:
		props									| errors
		[author: 'Stephen King', title: 'It']	| [:]
		[author: 'Dan Brown', title: 'It']		| [title: 'The only true author of It is Stephen King!']
		[author: 'Poe', title: 'It']			| [title: 'The only true author of It is Stephen King!', author: 'Author too short']
		[author: 'Poe', title: 'Pope']			| [author: 'Author too short']
	}
	
	def "Cache result internal"(){
		String kind = "book"
		String theKey = "test"
		String initial = "xyz"
		String changed = "abc"
		
		String key = EasyDSCachingCategory.getCustomKey(kind, theKey)
		
		expect:
		EasyDSCachingCategory.cacheInternal(key) { initial } == initial
		memcache.contains(key)
		memcache.get(key) == initial
		
		when:
		memcache.put(key, changed)
		
		then:
		EasyDSCachingCategory.cacheInternal(key) { initial } == changed
	}
	
	def "Cache result public"(){
		String kind = "book"
		String theKey = "test"
		String initial = "xyz"
		String changed = "abc"
		
		String key = EasyDSCachingCategory.getCustomKey(kind, theKey)
		
		expect:
		EasyDSCachingCategory.cache(kind, theKey) { initial } == initial
		memcache.contains(key)
		memcache.get(key) == initial
		
		when:
		memcache.put(key, changed)
		
		then:
		EasyDSCachingCategory.cache(kind, theKey) { initial } == changed
	}
	
	
	LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig(),
			new LocalMemcacheServiceTestConfig()
	)
	
	Key itKey
	Key daVinciKey
	MemcacheService memcache
	
	def setup(){
		helper.setUp()
		DatastoreService ds = DatastoreServiceFactory.datastoreService
		memcache = MemcacheServiceFactory.memcacheService
		itKey = ds.put(create('book', [author: 'Stephen King', title: 'It', published: 1987, owned: true]))
		daVinciKey = ds.put(create('book', [author: 'Dan Brown', title: 'Da Vinci Code', published: 2003, owned: false]))
		ds.put(create('book', [author: 'George Orwell', title: '1984', published: 1982, owned: false]))
		ds.put(create('movie', [director: 'Robert Zemeckis', title: 'Forrest Gump', released: 1994, owned: true]))
		ds.put(create('movie', [director: 'Frank Darabont', title: 'The Mist', released: 2007, owned: false]))
	}
	
	def cleanup(){
		helper.tearDown()
		itKey = null
		daVinciKey = null
	}
	
	Entity create(String kind, Map<String, Object> props){
		Entity en = new Entity(kind)
		for(Entry<String, Object> entry in props.entrySet()){
			en.setProperty(entry.key, entry.value)
		}
		en
	}
	
	
}
