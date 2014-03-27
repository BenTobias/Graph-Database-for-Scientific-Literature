CS3103 Concurrent Web Crawler Project
==================

## Execution
### Compilation
Include `jsoup-<version>.jar` in your Build Path.
Include `java-json.jar` in your Build Path.
Java Version: 1.7 JDK

### Run Program
Change values in `CrawlerSimulator.java` if necessary. Start crawler by running the `CrawlerSimulator` class.

Links will be written to `results.txt`.

### Database (Mac/Linux)

Run `mongod` or `mongod --dbpath <path_to_your_database_folder>` to activate mongodb.

Run main method in `DBManager.java`.

Check database using `mongo` command.

To persist a list of paper json arrays to database:

```
DBManager manager = new DBManager();
manager.insertDocuments(jsonStrings, new InsertDocumentsCallback(){
		@Override
		public void onFinish(List<String> failedUrls) {
			if(failedUrls.size() > 0) {
				// Handle failed docs here...
			}			
		}
	});
```



## Implementation
Described in `Master.java` class doc.


=======
CS3103-Project
==============

Graph Database