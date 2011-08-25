package groovyx.gaelyk.plugins.easyds

import java.util.Map.Entry;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import spock.lang.Specification

public class EasyDSCategorySpec extends Specification {

	
	def 'Count all entities'(){
		expect:
		EasyDSCategory.countAll(kind) == count
		where:
		kind			| count
		'book'			| 3
		"movi${'e'}"	| 2
		'music'			| 0
	}
	
	def 'Count by property'(){
		expect:
		EasyDSCategory.countBy(kind, props) == count
		where:
		kind 	| count | props
		'book'	| 2		| [owned: false]
		'movie'	| 1		| [owned: true]
		'book'	| 1		| [author: 'Dan Brown']
	}
	
	def 'Delete entity specified by map - id is number'(){
		when:
		EasyDSCategory.delete(book: itKey.id)
		
		then:
		EasyDSCategory.countAll('book') == 2
	}
	
	def 'Delete entity specified by map - id is list of numbers'(){
		when:
		EasyDSCategory.deleteAll(book: [itKey.id, daVinciKey.id])
		
		then:
		EasyDSCategory.countAll('book') == 1
	}
	
	def 'Entity exists'(){
		expect:
		EasyDSCategory.exists(book: itKey.id)
		EasyDSCategory.existsAll(book: [itKey.id, daVinciKey.id])
		!EasyDSCategory.exists(book: 123456)
	}
	
	def 'Fetch all entities by'(){
		expect:
		EasyDSCategory.fetchAllBy(kind, props).size() == count
		where:
		kind 	| count | props
		'book'	| 2		| [owned: false]
		'movie'	| 1		| [owned: true]
		'book'	| 1		| [author: 'Dan Brown']
		'book'	| 0		| [author: 'Karel Čapek']
	}
	
	def 'Fetch all offset'(){
		List<Entity> books = EasyDSCategory.fetchAllBy('book', [owned: false], [offset: 1])
		books.size() == 1
		books[0].getProperty('published') == 1982
	}
	
	def 'Fetch all max'(){
		List<Entity> books = EasyDSCategory.fetchAllBy('book', [owned: false], [max: 1])
		books.size() == 1
		books[0].getProperty('published') == 2003
	}
	
	def 'Fetch all sorted'(){
		List<Entity> books = EasyDSCategory.fetchAllBy('book', [owned: false], [sort: published])
		books.size() == 2
		books[0].getProperty('published') == 1982
		books[1].getProperty('published') == 2003
	}
	
	def 'Fetch all sorted desc'(){
		List<Entity> books = EasyDSCategory.fetchAllBy('book', [owned: false], [sort: published, order: 'desc'])
		books.size() == 2
		books[0].getProperty('published') == 2003
		books[1].getProperty('published') == 1982
	}
	
	def 'Fetch entity by'(){
		expect:
		EasyDSCategory.fetchBy('movie', [owned: true]).getProperty('released') 	== 1994
		EasyDSCategory.fetchBy('book', [owned: false]).getProperty('published') == 2003
		EasyDSCategory.fetchBy('book', [author: 'Karek Čapek'])					== null
	}
	
	def 'Fetch by id'(){
		expect:
		EasyDSCategory.fetch(book: itKey.id).getProperty('published') == 1987
		EasyDSCategory.fetchAll(book: [itKey.id, daVinciKey.id]).size() == 2
	}
	
	
	def 'List all entities'(){
		expect:
		EasyDSCategory.list(kind).size() == count
		
		where:
		kind			| count
		'book'			| 3
		"movi${'e'}"	| 2
		'music'			| 0
	}
	
	def 'Create entity'(){
		expect:
		EasyDSCategory.countAll('book') == 3
		EasyDSCategory.create('book', [author: 'Stephen King', title: 'Cujo', published: 1993, owned: false])
		EasyDSCategory.countAll('book') == 4
	}
	
	def 'Update map entity'(){
		EasyDSCategory.update([book: itKey.id], [published: 1990])
		expect:
		EasyDSCategory.fetch([book: itKey.id]).getProperty('published') == 1990
	}
	
	def 'Update entity'(){
		EasyDSCategory.update(EasyDSCategory.fetch([book: itKey.id]), [published: 1990])
		expect:
		EasyDSCategory.fetch([book: itKey.id]).getProperty('published') == 1990
	}
	
	def 'Update map entity if not value set'(){
		EasyDSCategory.updateIfNotSet([book: itKey.id], [published: 1990, haluz: true])
		expect:
		EasyDSCategory.fetch([book: itKey.id]).getProperty('published') == 1987
		EasyDSCategory.fetch([book: itKey.id]).getProperty('haluz') == true
	}
	
	def 'Update entity if not value set'(){
		EasyDSCategory.updateIfNotSet(EasyDSCategory.fetch([book: itKey.id]), [published: 1990, haluz: true])
		expect:
		EasyDSCategory.fetch([book: itKey.id]).getProperty('published') == 1987
		EasyDSCategory.fetch([book: itKey.id]).getProperty('haluz') == true
	}
	
	def 'Validate entity'(){
		def trueKing = { value, entity ->
				if(value == 'It' && entity.author != 'Stephen King'){
					return 'The only true author of It is Stephen King!'
				}
		}
		
		def authorLong = {
				if(it.size() < 4){
					return 'Author too short'
				}
		}
		
		expect:
		
		!trueKing('It', [author: 'Stephen King', title: 'It'])
		trueKing('It', [author: 'Poe', title: 'It']) == 'The only true author of It is Stephen King!'
		!authorLong('King')
		authorLong('Poe') == 'Author too short'
		
		EasyDSCategory.validate(EasyDSCategory.create('book', props), [title: trueKing, author: authorLong]) == errors
		
		where:
		props									| errors
		[author: 'Stephen King', title: 'It']	| [:]
		[author: 'Dan Brown', title: 'It']		| [title: 'The only true author of It is Stephen King!']
		[author: 'Poe', title: 'It']			| [title: 'The only true author of It is Stephen King!', author: 'Author too short']
		[author: 'Poe', title: 'Pope']			| [author: 'Author too short']
	}
	
	
	LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
	)
	
	Key itKey
	Key daVinciKey
	
	def setup(){
		helper.setUp()
		DatastoreService ds = DatastoreServiceFactory.datastoreService
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
