
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
	Paper.find(queryObj)
		 .populate('citations', {'title':1, 'authors':1})
		 .exec(function(err, papers) {
			if(err) {
				console.error('Unable to retrieve results from search: ' + err);
			}
			callback(papers);
		});
};


//////////////////////
//    Public API    //
/////////////////////


exports.getAuthor = function(id, callback) {
	Author.findOne({_id: id}, function(err, results){
		callback(results);
	});
}

exports.getPaper = function(id, callback) {
	Paper.findOne({_id: id}, function(err, results){
		callback(results);
	});
}

exports.populateAuthorNamesJson = function() {

	var fs = require('fs');
	var pth = 'authornames.json';

	fs.exists(pth, function(exists){

		if (!exists) {

			Author.find({}, {'_id': 0, 'name':1}, function(err, authorNames){

				authorNames = authorNames.map(function(e) {
					return '"' + e.name + '"';
				});

				fs.writeFile(pth, authorNames, function(err) {
					if(err) console.log(err);
					else console.log('written author names to json file');
				});
			});
		}	
	});
};

exports.populatePaperTitlesJson = function() {

	var fs = require('fs');
	var pth = 'papertitles.json';

	fs.exists(pth, function(exists){

		if (!exists) {

			Paper.find({citations:{$exists:true}}, {'_id': 0, 'title':1}, function(err, results){

				results = results.map(function(e) {
					var str = e.title.split('"').join('');
					return '"' + str + '"';
				});

				fs.writeFile(pth, results, function(err) {
					if(err) console.log(err);
					else console.log('written author names to json file');
				});
			});
		}	
	});
};

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

exports.findPapersWithSimilarCitation = function(theTitle, callback) {
	Paper.find({title:theTitle}, 'citations')
		.populate('citations', 'cited_by')
		.exec(function(err, papers){
			var citations = papers[0].citations;
			
			var similarPaperIds = [];
			citations.forEach(function(citedPaper){
				var citedBy = citedPaper.get("cited_by");
		
				citedBy.forEach(function(c) {
					if(similarPaperIds.indexOf(c) == -1) {
						similarPaperIds.push(c);
					}	
				});
			});
			
			Paper.find({_id:{$in:similarPaperIds}, title:{$ne:theTitle}})
				.populate('citations')
				.exec(function(err, papers){
					callback({'citations':citations.map(function(c){return c._id;}), 'papers': papers});
			});
	});
};



