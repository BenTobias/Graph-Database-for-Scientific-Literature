
var global = require('./global');

exports.index = function(req, res){
  	res.render('index');
};

exports.simpleSearch = function(req, res){
	var title = req.body.title.trim();
	var author = req.body.author.trim();
	global.filterPaperByTitleAndAuthor(title, author, function(result){
		res.render('result', {'data': result});
	});
};

var handleSearchCallback = function(res) {
	return res.send(res);
};

exports.similarCitation = function(req, res){
	var doi = req.body.doi;
	global.findPapersWithSimilarCitation(doi, function(results){
		console.log('similarCitation: ', results);
		res.send(results);
	});
};



