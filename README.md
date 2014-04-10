CS3103 Concurrent Web Crawler Project
==================
## Description


This project consists of two Java projects in folder `Coordinator/` and `Crawler/` and a Node.js project in folder `QueryEngine/`.

The Java projects are used for crawling, parsing and populating database. Node.js project is created for displaying crawled results.

The project is running on <http://group05-i.comp.nus.edu.sg>

## Execution

### File Organization
The `Coordinator/` and `Crawler/` folders should be set up as 2 separate projects.

### Compilation

1. Coordinator
	- Create a Java project any IDE. 
	- Drag all 3 Java files from `Coordinator/` folder into the project.
	- Include `java-json.jar` and `mongo-<version>.jar`.

2. Crawler
 	- Create a Java project any IDE. 
	- Drag all 4 Java files from `Crawler/` folder into the project.
	- Include `jsoup-<version>.jar`.

### Run Program
1. Active Mongo DB
	- Run `mongod` or `mongod --dbpath <path to your database folder>`
	- Run `mongo` to access database.
2. Run Coordinator
	- In Coordinator project, run `Coordinator.java` and then stop it. This is to get `Coordinator.class` file.
	- Navigate to your workspace folder.
	- Start Coordinator by `java -cp ".;java-json.jar;mongo-2.10.1.jar" Coordinator`
	- Mac users `java -cp ".:../lib/*" Coordinator`. (Suppose your workspace folder has `src/`, `bin/` and `lib/` where `lib/` folder has all jar files).

3. Run Crawler
	- Start crawler by running the `CrawlerSimulator.java` in IDE.
	
4. Run Query Engine
	- `cd` into `QueryEngine/` folder
	- `npm install`
	- `sudo npm install -g bower`
	- `bower install`. You should see a `bower_components` folder in `public`.
	- `node app.js`

### Run Program on VM
1. `ssh sadm@group05-i.comp.nus.edu.sg`
2. `su` to root
3. Start Mongo DB `bash db.sh`
4. Start Coordinator 
	- `cd` into Coordinator folder
	- `javac -Xlint -cp ".:../*" Coordinator.java`
	- `nohup java -cp ".:../*" Coordinator </dev/null 2>&1 | tee logfile.log &`
5. Start CrawlerSimulator
	- `cd` into Crawler folder
	- `javac -Xlint -cp ".:../*" CrawlerSimulator.java`
	- `nohup java -cp ".:../*" CrawlerSimulator </dev/null 2>&1 | tee logfile.log &`

6. Start QueryEngine
	- `bash run.sh`

### Trouble shooting 

#### Mongo DB 

If the database is downloaded from somewhere else, it needs to be repaired before using.

**Repair Mongo database downloaded from VM**

`mongod --dbpath <path to your database folder> --repair --nojournal`

**Run Mongo DB**

`mongod --dbpath <path to your database folder>`

If not successful, remove `mongod.lock` file and run 

`mongod --dbpath <path to your database folder> --repair`


