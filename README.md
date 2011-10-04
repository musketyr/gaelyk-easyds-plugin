#Gaelyk Easy Datastore Service Plugin (EasyDS)

## Overview
 Gaelyk EasyDS Plugin is inspired by [GORM](http://grails.org/doc/latest/guide/5.%20Object%20Relational%20Mapping%20\(GORM\).html)
 methods on [Grails](http://grails.org) domain classes. It provides basic [CRUD](http://en.wikipedia.org/wiki/Create,_read,_update_and_delete)
 and validation support for basic app engine entities. Because the app engine datastore
 entites are distinguished by their kinds and keys the most of the methods are added to the String and Map classes.
 If there is an option 
 
 This plugin is expected to use for basic datastore operations. For more sophisticated use e.g.
 [Obgaektify](http://obgaektify.appspot.com/) or other ORM framework.
 
 This plugin provides [DSLD](http://en.appsatori.eu/2011/05/writing-groovy-dsl-descriptors-dsld-for.html) 
 file for better [STS](http://www.springsource.com/developer/sts) ([Eclipse](http://www.eclipse.org )) support.
 
## Instalation
The easiest way how to install the plugin using [gradle gaelyk plugin 0.2+](https://github.com/bmuschko/gradle-gaelyk-plugin)
which is able to download and install the plugin by following command

    gradle gaelykInstallPlugin -Pplugin=easyds

Otherwise you can just download the archive and unpack it into your project's directory. The plugin expects 
your source codes lives in `src` directory. If you are using the new layout, copy `easyDS.dsdl` from `src` folder
into `src\main\groovy` one to get the code completition in STS.
 
## Create entities
 To create new entity just call the `create` method on the string representing the entity kind and supply
 map of new entity parameters as the arguments.
 
```groovy
  Entity book = 'book'.create(author: 'Stephen King', title: 'It', description: 'Oh, my! This is scary!')
```
## Reading entities
 There are several approaches how to retrive entites from the datastore. The most simple is to retrive
 them by their identifier.
### Fetching by id
 To fetch entity from datastore just call the method `fetch` on single map with single entry.
 The key specifies the kind and the value the id. The id could be any object which can be coereced to long eg. string.
 
```groovy
  Entity book = [book: 15].fetch()
```
 You can also fetch more then one entity by providing a list of identifiers as a value of the single map entry
 and calling the `fetchAll` method.

```groovy
  List<Entity> books = [book: [14,15,16]].fetchAll()
```

### Fetching all entities of given kind
 To fetch all entities of given kind just call the `list` method on the string representing the entity kind.
 
```groovy
  List<Entity> books = 'book'.list()
```

You can cusomize result list by calling the `list` method with map of configurations. Available customizations
are

 * `max` - the maximum size of result list as int
 * `offset` - the offset of the result list as int
 * `sort` - the property to be used for sorting the list
 * `order` - the sort direction - use `desc` or `SortDirection.DESCENDING` to swich default ascending order to descending

```groovy
  List<Entity> tenBooks = 'book'.list(max:10)
  List<Entity> tenBooksFrom20 = 'book'.list(max:10, offset: 20)
  List<Entity> booksOrderByAuthor = 'book'.list(sort: 'author')
  List<Entity> booksOrderByAuthorDesc = 'book'.list(sort: 'author', order: 'desc')
```

### Simple quering
You can retrieve entities using the simple queries against their properties. There is two similar method
`fetchBy` and `fetchAllBy` on string which only differs by their number of entities returned. The `fetchBy` returns
only first entity found and the `fetchAllBy` method returns all the results. The properties supplied in the
map are searched for equality. You can also use customization map as for `list` method as the second argument.

```groovy
  Entity firstKingsBook = 'book'.fetchBy(autor: 'Stephen King')
  List<Entity> kingsBooksByTitle = 'book'.fetchAllBy(author: 'Stephen King', [sort: 'title']) 
```

### Counting entities
You can simply obtain count of entites in the database by using the `countAll` method on the
string representing the entity kind. You can also get count of the simple query result using
the `countBy` method the same way.

```groovy
  int booksCount = 'book'.countAll()
  int kingsBooksCount = 'book'.countBy(author: 'Stephen King')
```

### Checking existing entities by identifier
You can check easily if there is corresponding entity in the datastore for given id or ids using the
`exists` or `existAll` method. The method is called on the map having the same form as in the `fetch` and `fetchAll`
example.

```groovy
  boolean itExists = [book: 15].exists()
  boolean theyExist = [book: [15, 20, 30]].existAll()
```
## Updating entities
You can update entity properties by calling the `update` method on the map. The map must have the same
form as in the `fetch` example. The `update` method works on entities themselves too.

```groovy
  Entity updated = [book: 15].update(author: 'Paul King')
  updated.update(author: 'Me Myself I.')
```

### Conditional updates for unset properties
Sometimes you want set properties only if they weren't set before. You can use the `updateIfNotSet` method
on maps and entities in the same manner as the `update` method. The `entity.hasPropery(propertyName)` method
is used to determine whether the property is set.

```groovy
  Entity book = 'book'.create(author: 'Stephen King')
  book.updateIfNotSet(title: 'It') // the title is 'It' now
  book.updateIfNotSet(title: 'Cujo') // the title is still the same, because it was already set
```

## Deleting entities
To delete entity or entities just use the `delete` or `deleteAll` method on the map. The map must have the same
form as in the `fetch` and `fetchAll`example. 

```groovy
 [book: 15].delete()
 [book: [11, 12, 13]].deleteAll()
```

## Entity validation
You can validate your entity or map using the `validate` method. You call the method with map as an argument.
The map contains validatiors closures. The key in the map of validators must be the same as the name of property
or entry you want to validate. The validator closure must accept one or two parameters. If the closure
accepts one parameter the value of the validated property is supplied. If the closure accepts two parameters
the property value is sent as the first parameter and the map of all properties is supplied as the second parameter.
The method returns map of errors. Any non-null return value from the closure is supposed to be an error. 
You usually want to return the error message as string.

```groovy
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
  
  def errors = [title: 'The only true author of It is Stephen King!', author: 'Author too short']
  
  [author: 'Poe', title: 'It'].validate(title: trueKing, author: authorShort) == errors
  [author: 'Stephen King', title: 'It'].validate(title: trueKing, author: authorShort) == [:]
  
```


## Roadmap
 
 * 0.1 - Basic CRUD operations and validation
 * 0.2 - Ability to cache entities
 * 0.3 - Reusable and combinable validations
