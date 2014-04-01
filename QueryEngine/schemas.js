var mongoose = require('mongoose'),
	ctemp = require('./correspondence'),
	Correspondence = mongoose.model('Correspondence'),
	Schema = mongoose.Schema;
	

//////////////////
//    Schema    //
//////////////////

var AuthorSchema = Schema({
	papers: [{type: Schema.Types.ObjectId, ref:"Paper"}],	

});

var PaperSchema = Schema({
	authors: [{type: Schema.Types.ObjectId, ref:"Author"}],
});


var Author = mongoose.model('Author', AuthorSchema);
var Paper = mongoose.model('Paper', PaperSchema);




