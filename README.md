CS3103 Concurrent Web Crawler Project
==================

## Execution

### Compilation
1. Coordinator
	- Include `java-json.jar` and `mongo-<version>.jar`.

2. Crawler
	- Include `jsoup-<version>.jar`.

## File Organization
The `Coordinator` and `Crawler` folders should be set up as 2 separate projects.

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



## Node Query Engine

**Repair Mongo database downloaded from VM**

`mongod --dbpath <path to your database folder> --repair --nojournal`

**Run Mongo DB**

`mongod --dbpath <path to your database folder>`

If not successful, remove `mongod.lock` file and run 

`mongod --dbpath <path to your database folder> --repair`

**Configure Node Project**

`npm install`

`sudo npm install -g bower`

`bower install`

You should see a `bower_components` folder in `public`.

**Run locally**

`node app.js`

## Related links

<http://mongoosejs.com>

<http://jade-lang.com>

<http://expressjs.com>
