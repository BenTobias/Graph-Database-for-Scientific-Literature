
var mongoose = require('mongoose');

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

exports.filterPaperBy = function(paper, author, yearFrom, yearTo) {
	// TODO: Add where statements
	Paper.find({}, 'title', function(err, papers) {	
		if(err) 
			console.log('Error retrieving papers in simpleSearch: ' + err);
		console.log('papers: ', papers);
		return papers;
	});
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



