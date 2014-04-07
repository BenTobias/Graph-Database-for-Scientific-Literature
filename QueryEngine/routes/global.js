
var mongoose = require('mongoose');
var async = require('async');
var ObjectId = mongoose.Types.ObjectId;
var parser = require('./parser');

var Schema = mongoose.Schema;
	

//////////////////
//    Schema    //
//////////////////

var Author = mongoose.model('Author', 
        new Schema({
			name: String,
			createdDate: { type: Date, default: Date.now },
			papers: [{type: Schema.Types.ObjectId, ref:"Paper"}],
			coauthors:[{type: Schema.Types.ObjectId, ref:"Author"}]
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
			authors: [{
				id: {type: Schema.Types.ObjectId, ref:"Author"}, 
				name: String
			}],
			citations: [{type: Schema.Types.ObjectId, ref:"Paper"}]
		}), 'paper');


var parseQuery = function(string, key) {
	var parsedQuery = parser.parseBooleanQuery(string, key);

	if ((parsedQuery == undefined) || (typeof(parsedQuery) == 'string')) {
		console.log('key is: ' + key);
		// use original string
		parsedQuery = {};
		parsedQuery[key] = {'$regex': '.*' + string + '.*', '$options': 'i'};
	}

	return parsedQuery;
};


var executeSearchQuery = function (queryObj, callback) {
	Paper.find(queryObj, function(err, papers) {
		if(err) {
			console.error('Unable to retrieve results from search: ' + err);
		}
		callback(papers);
	});
	// callback(queryObj); // uncomment to see query
};


//////////////////////
//    Public API    //
/////////////////////

exports.filterPaperByTitleAndAuthor = function(title, author, callback) {

	var queryTitle = {},
		queryAuthor = {},
		queryObj = {};

	if(title)
		queryTitle = parseQuery(title, 'title');

	if(author)
		queryAuthor = parseQuery(author, 'authors.name');

	queryObj = {$and:[queryTitle, queryAuthor]};
	
	executeSearchQuery(queryObj, callback);
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



