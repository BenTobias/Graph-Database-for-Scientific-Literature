
/**
 * Module dependencies.
 */

var express = require('express');
var routes = require('./routes');
var http = require('http');
var path = require('path');

var app = express();

// connect to db
var global = require('./routes/global');
var mongoose = require('mongoose');
mongoose.connect('mongodb://localhost/cs3103project');
var db = mongoose.connection;
db.on('error', console.error.bind(console, 'connection error:'));
db.once('open', function callback(){
	console.log('connected to db');
});

// all environments
app.set('port', process.env.PORT || 3000);
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

app.get('/', routes.index);
app.get('/author/:authorid', routes.getAuthor);
app.get('/paper/:paperid', routes.getPaper);
app.get('/authornames.json', routes.getAuthorNames);
app.get('/papertitles.json', routes.getPaperTitles);
app.post('/simpleSearch', routes.simpleSearch);
app.post('/similarCitationPaper', routes.similarCitation);

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
