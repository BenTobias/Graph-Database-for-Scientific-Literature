
var mongoose = require('mongoose');

var Schema = mongoose.Schema;
	

//////////////////
//    Schema    //
//////////////////

var AuthorSchema = Schema({
	name: String,
	createdDate: { type: Date, default: Date.now },
	papers: [{type: Schema.Types.ObjectId, ref:"Paper"}]
});

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
});


var Author = mongoose.model('Author', AuthorSchema);
var Paper = mongoose.model('Paper', PaperSchema);

exports.filterPaperBy = function(paper, author, yearFrom, yearTo) {
	Paper.find({}, 'title', function(err, papers) {	
		if(err) 
			console.log('Error retrieving papers in simpleSearch: ' + err);
		console.log('papers: ', papers);
	});
};