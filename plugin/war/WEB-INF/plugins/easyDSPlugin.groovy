import groovyx.gaelyk.plugins.easyds.EasyDSCategory
import groovyx.gaelyk.plugins.easyds.EasyDSCachingCategory

// use one of following categories depending if you want to enable cache or not

// cache is not enabled by default for backward comaptibility reasons, but
// you should consider using cache because of the high price of datastore operations
categories EasyDSCategory

// to enable cache, comment the previous categories declaration and uncomment the following one
// categories EasyDSCachingCategory

