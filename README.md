CS3103 Concurrent Web Crawler Project
==================

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

`bower install bootstrap`

`bower install typeahead.js`

`bower install backbone`

You should see a `bower_components` folder in `public`.

**Run locally**

`node app.js`

## Related links

<http://mongoosejs.com>

<http://jade-lang.com>

<http://expressjs.com>