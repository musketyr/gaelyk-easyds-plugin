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
 The key specifies the kind and the value the id.
 
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

YOu can cusomize result list by calling the `list` method with map of configurations. Available customizations
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

 
## Roadmap
 
 * 0.1 - Basic CRUD operations
 * 0.2 - Reusable validations