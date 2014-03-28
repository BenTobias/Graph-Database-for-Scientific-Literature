CS3103 Concurrent Web Crawler Project
==================

## Execution

### Compilation
1. Coordinator
	- Include `java-json.jar` and `mongo-<version>.jar`.

2. Crawler
	- Include `jsoup-<version>.jar`.

### Run Program
1. Active Mongo DB
	- Run `mongod` or `mongod --dbpath <path to your database folder>`
	- Run `mongo` to access database.
2. Run Coordinator
	- Compile Coordinator, by running it once in eclipse.
	- Navigate to your eclipse workspace folder.
	- Start Coordinator by `java -cp ".;java-json.jar;mongo-2.10.1.jar" Coordinator`
	- Mac users `java -cp ".:../lib/*" Coordinator`. (Suppose your workspace folder has `src/`, `bin/` and `lib/` where `lib/` folder has all jar files).

3. Run Crawler
	- Change values in `CrawlerSimulator.java` if necessary.
	- Start crawler by running the `CrawlerSimulator` class.

Links will be written to `results.txt`.

## Implementation
Described in `Master.java` class doc.


=======
CS3103-Project
==============

Graph Database