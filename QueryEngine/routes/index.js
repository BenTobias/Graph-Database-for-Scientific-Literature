
var global = require('./global');

exports.index = function(req, res){
  	res.render('index');
};

exports.simpleSearch = function(req, res){

	var paper = req.body.paper.trim();
	var author = req.body.author.trim();
	var yearFrom = req.body.yearFrom.trim();
	var yearTo = req.body.yearTo.trim();

	var results = global.filterPaperBy(paper, author, yearFrom, yearTo);
	res.send({data:results});
};

exports.similarCitation = function(req, res){
	var doi = req.body.doi;
	var results = global.findPapersWithSimilarCitation(doi);
	res.send(results);
};



