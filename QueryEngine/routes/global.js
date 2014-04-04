
var mongoose = require('mongoose');
var async = require('async');
var ObjectId = mongoose.Types.ObjectId;
var parser = require('./parser');

var Schema = mongoose.Schema;
	

//////////////////
//    Schema    //
//////////////////

var AuthorSchema = Schema({
	name: String,
	createdDate: { type: Date, default: Date.now },
	papers: [{type: Schema.Types.ObjectId, ref:"Paper"}]
}, { collection : 'author' });

var PaperSchema = Schema({
	title: String,
	doi: String,
	url: String,
	downloadlinks: { type: [String], default: [] },
	last_crawled: { type: Date, default: Date.now },
	abstract: String,
	year: { type: Number, min: 0, max: 2015 },
	authors: [{type: Schema.Types.ObjectId, ref:"Author"}],
	citations: [{type: Schema.Types.ObjectId, ref:"Paper"}]
}, { collection : 'paper' });

// var Author = mongoose.model('author', AuthorSchema);
// var Paper = mongoose.model('paper', PaperSchema);

var Author = mongoose.model('Author', 
        new Schema({
			name: String,
			createdDate: { type: Date, default: Date.now },
			papers: [{type: Schema.Types.ObjectId, ref:"Paper"}]
		}), 'author');

var Paper = mongoose.model('Paper', 
        new Schema({
			title: String,
			doi: String,
			url: String,
			downloadlinks: { type: [String], default: [] },
			last_crawled: { type: Date, default: Date.now },
			abstract: String,
			year: { type: Number, min: 0, max: 2015 },
			authors: [{type: Schema.Types.ObjectId, ref:"Author"}],
			citations: [{type: Schema.Types.ObjectId, ref:"Paper"}]
		}), 'paper');

var authorToObjectidMap = {};

var createWildcardQuery = function (key, string) {
	var query = {};
	query[key] = {'$regex': '.*' + string + '.*', '$options': 'i'};
	return query;
};

var parseQuery = function(string, key) {
	var parsedQuery = parser.parseBooleanQuery(string, key);

	if ((parsedQuery == undefined) || (typeof(parsedQuery) == 'string')) {
		// use original string
		parsedQuery = createWildcardQuery(key, string);
	}

	return parsedQuery;
};

var parseAuthorQuery = function(query) {
	var tokens = parser.getTokensList(query);
	var processedTokens = [];

	async.map(tokens,
		function (token, callback) {
			if (isOperator(token)) {
				return callback(null, token);
			}

			getAuthorOidFromDB('bhojan', function(err, res) {
				return callback(err, res);
			});
		},
		function (err, res) {
			console.log(res);
		}
	);
};

/**
 * Checks if the string is an operator.
 *
 * @param string {string}: .
 * @return {boolean}: true if the string is an operator, false otherwise.
 * @private
 */
var isOperator = function (string) {
	return ['&&', '||'].indexOf(string) != -1;
};

/**
 * Gets the list of author ObjectIds from the database. The query is a
 * wildcard query based on the author string. This means that more than
 * 1 author could be return if the string exists in the database.
 *
 * @param author {string}: the author name string to query.
 * @param callback {Function}: the callback function to call when the query
 * 		is completed.
 * @private
 */
var getAuthorOidFromDB = function (author, callback) {
	var parsedAuthor = createWildcardQuery('name', author);

	Author.find(parsedAuthor, '_id', function(err, authorid) {	
		if(err) 
			console.error('Unable to retrieve results from search: ' + err);
			return callback(err, null);
		console.log('author: ', authorid);
		// callback(authorid);
		return callback(null, authorid);
	});
};


//////////////////////
//    Public API    //
/////////////////////

exports.filterPaperBy = function(title, author, yearFrom, yearTo, callback) {
	// TODO: Add where statements
	// var parsedTitle = parseQuery(title, 'title');
	var parsedAuthor = parseAuthorQuery(author);
	// Paper.find(parsedTitle, 'title', function(err, papers) {	
	// 	if(err) 
	// 		console.error('Unable to retrieve results from search: ' + err);
	// 	console.log('papers: ', papers);
	// 	callback(papers);
	// });
};

exports.findPapersWithSimilarCitation = function(theDoi, callback) {
	Paper.find({doi:theDoi}, 'citations')
		.populate('citations', 'cited_by')
		.exec(function(err, thisPaper){
		if(err) 
			console.log('Error retrieving papers with similar citation: ' + err);
		else {
			var citations = thisPaper[0]["citations"];
			// console.log('cititions: ' + citations);
			var similarPaperIds = [];
			citations.forEach(function(paper){
				console.log("paper: " + paper);
				var cited_by = paper["cited_by"]; // Why are you undefined?!
				cited_by.forEach(function(c) {
					if(similarPaperIds.indexOf(c) == -1) {
						similarPaperIds.push(c);
					}	
				});
			});
			callback(similarPaperIds);
		}
	});
};



