
var global = require('./global');

exports.index = function(req, res){
  	res.render('index');
  	global.populateAuthorNamesJson();
  	global.populatePaperTitlesJson();
};

exports.getAuthor = function(req, res){
	var id = req.params.authorid;
	global.getAuthor(id, function(result){
		res.send(result);
	});
}

exports.getPaper = function(req, res){
	var id = req.params.paperid;
	global.getPaper(id, function(result){
		res.send(result);
	});
}

exports.getAuthorNames = function(req, res){
  	res.sendfile('authornames.json');
};

exports.getPaperTitles = function(req, res){
  	res.sendfile('papertitles.json');
};

exports.simpleSearch = function(req, res){
	var title = req.body.title.trim();
	var author = req.body.author.trim();
	global.filterPaperByTitleAndAuthor(title, author, function(result){
		res.send({'title':title, 'author':author, 'data':result});
	});
};

exports.similarCitation = function(req, res){
	var title = req.body.title;
	global.findPapersWithSimilarCitation(title, function(results){
		res.send({'title':title, 'data':results});
	});
};

exports.collaborationDistance = function(req, res) {
	var authorTo = req.body.authorTo;
	var authorFrom = req.body.authorFrom;

	global.getShortestPathBetweenAuthors(authorTo, authorFrom, function(result) {
		res.send(result);
	});
};
