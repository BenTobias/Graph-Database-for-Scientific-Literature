
var global = require('./global');

exports.index = function(req, res){
  	res.render('index');
};

exports.simpleSearch = function(req, res){
	var title = req.body.title.trim();
	var author = req.body.author.trim();
	var yearFrom = req.body.yearFrom.trim();
	var yearTo = req.body.yearTo.trim();

	var results = global.filterPaperBy(title, author, yearFrom, yearTo,
		handleSearchCallback(res));
};

var handleSearchCallback = function(res) {
	var helperFunction = function(results) {
		res.render('index', {data:results});
	};

	return helperFunction;
};

exports.similarCitation = function(req, res){
	var doi = req.body.doi;
	global.findPapersWithSimilarCitation(doi, function(results){
		console.log('similarCitation: ', results);
		res.send(results);
	});
};



