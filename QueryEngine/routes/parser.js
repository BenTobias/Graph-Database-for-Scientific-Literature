/**
 * The namespace for the parser class.
 * @namespace
 */
var Parser = Parser || {};

/**
 * The list of operators that the parser handles.
 * @type {Array.<string>}
 * @private
 */
Parser.operators = ['&&', '||'];

/**
 * Parses the boolean query string into a mongo query object.
 *
 * The result can be run directly as a mongo query. If the query string
 * is invalid, an error will be printed and a null object will be returned.
 * 
 * @param query {string}: the query string to parse.
 * @param key {string}: the field that the query belongs to.
 * @return {Object|undefined}: the mongo query object or undefined (if the
 *      string cannot be parsed).
 */
Parser.parseBooleanQuery = function (query, key) {
    var isBooleanQuery = ((query.indexOf('&&') != -1) ||
        (query.indexOf('||') != -1));

    if (!isBooleanQuery) {
        return query;
    }

    var queryTokens = query.split(' ');
    queryTokens = this.concatWordsFilterEmptyStrings(queryTokens);


    this.operators.forEach(function(operator) {
        queryTokens = this.handleBinaryOperator(operator, key, queryTokens);
    }, this);

    if (queryTokens == undefined) {
        return;
    }

    return queryTokens[0]
};

/**
 * Concats the space-separated terms and removes empty strings.
 *
 * Example: ['A', 'B', 'C', '&&', 'D'] -> ['A B C', '&&', 'D']
 * Example: ['A', '', '', '&&', 'B'] -> ['A', '&&', 'B']
 *
 * @param tokens {Array.<string>}: the array of space-separated terms.
 * @return {Array.<string>}: the processed tokens array.
 * @private
 */
Parser.concatWordsFilterEmptyStrings = function (tokens) {
    var processedTokens = [];
    var concatFlag = false;

    for (var i = 0, token; token = tokens[i], i < tokens.length; i++) {
        if (token == '') {
            continue;
        }

        if (this.operators.indexOf(token) != -1) {
            concatFlag = false;
        }
        else if (concatFlag == true) {
            token = processedTokens.pop() + ' ' + token;
        }
        else {
            concatFlag = true;
        }

        processedTokens.push(token);
    }

    return processedTokens;
};

/**
 * Handles the construction of a query object for a binary operator.
 *
 * @param operator {string}: the operator to handle (eg. '&&').
 * @param key {string}: the field that the query belongs to.
 * @param queryTokens {Array.<string|Object>}: the list of tokens to
 *      convert to a mongo query object.
 * @return {Array.<string|Object>}: the parsed list of tokens.
 * @private
 */
Parser.handleBinaryOperator = function (operator, key, queryTokens) {
    if (queryTokens == undefined) {
        return;
    }

    var constructFlag = false;
    var newQueryTokens = [];

    for (var i = 0; i < queryTokens.length; i++) {
        var token = queryTokens[i];
        if ((this.operators.indexOf(token) != -1) && constructFlag){
            console.error('Operators cannot be adjacent to each other.');
            return;
        }

        if (token == operator) {
            constructFlag = true;
        }
        else if (constructFlag) {
            newQueryTokens.pop();
            var t1 = newQueryTokens.pop();
            var t2 = token;

            if (t1 == undefined) {
                console.log('Binary operator needs 2 elements.');
                return;
            }

            var token = this.createDBQueryObject(t1, t2, key, operator);
            constructFlag = false;
        }

        newQueryTokens.push(token);
    };

    if (constructFlag) {
        console.error('Query string cannot end with a binary operator.');
        return;
    }

    return newQueryTokens
};

/**
 * Creates a query object.
 * 
 * The query object should be in this format:
 *  eg. {'$and': [{'title': 'hi'}, {'title': 'bye'}]}
 *
 * @param t1 {string|Object}: the term to include in the query object.
 *      This term is either a token string or a previously created query
 *      object.
 * @param t2 {string|Object}: the term to include in the query object.
 *      This term is either a token string or a previously created query
 *      object.
 * @param key {string}: the field that the query belongs to.
 * @param operator {string}: the operator to handle (eg. '&&').
 * @return {Array.<string|Object>}: the parsed list of tokens.
 * @private
 */
Parser.createDBQueryObject = function (t1, t2, key, operator) {
    var dbOperator = '';
    if (operator == '&&') {
        dbOperator = '$and';
    }
    else if (operator == '||') {
        dbOperator = '$or';
    }
    else {
        console.error('No such operator: ' + operator);
        return;
    }

    var queryList = [];
    queryList = this.addTermToQueryList(t1, key, queryList, dbOperator);
    queryList = this.addTermToQueryList(t2, key, queryList, dbOperator);

    var queryObj = {};
    queryObj[dbOperator] = queryList;

    return queryObj;
};

/**
 * Adds the term to the query object. If the operator of the term is the same
 * as the current operator, concat all arguments with the current query list.
 * 
 * If the term is a string, place it in a term object with its associated key
 * and options.
 *
 * @param term {string|Object}: the term to include in the query object.
 *      This term is either a token string or a previously created query
 *      object.
 * @param key {string}: the field that the query belongs to.
 * @param queryList {Array.<Object>}: the list of term objects for the query
 *      object.
 * @param dbOperator {string}: the operator to compare with (eg. '$and').
 * @return {Array.<Object>}: the updated list of term objects.
 * @private
 */
Parser.addTermToQueryList = function (term, key, queryList, dbOperator) {
    // Flattens the objects by combining all the similar operators together.
    if (typeof(term) == 'object' && term[dbOperator]) {
        queryList = queryList.concat(term[dbOperator]);
    }
    else if (typeof(term) == 'object') {
        queryList.push(term);
    }
    else {
        var termObj = this.createTermObject(key, term);
        queryList.push(termObj);
    }

    return queryList;
};

/**
 * Creates the term object. This includes the wildcard queries and other
 * query options.
 *
 * @param key {string}: the field that the query belongs to.
 * @param term {string}: the term to query.
 * @return {Object}: the term object.
 * @private
 */
Parser.createTermObject = function(key, term) {
    var termObj = {};
    termObj[key] = {'$regex': '.*' + term + '.*', '$options': 'i'};
    return termObj;
};

console.log(Parser.parseBooleanQuery('A B C', 'title'));
console.log(Parser.parseBooleanQuery('A B && C', 'title'));
console.log(Parser.parseBooleanQuery('A && B &&     C   ', 'title'));
console.log(Parser.parseBooleanQuery('|| A B && C', 'title'));
console.log(Parser.parseBooleanQuery('A B &&', 'title'));
console.log(Parser.parseBooleanQuery('A B && || C', 'title'));
console.log(Parser.parseBooleanQuery('A && B && C || D || E', 'title'));
console.log(Parser.parseBooleanQuery('A || B', 'title'));
console.log(Parser.parseBooleanQuery('A && B || C && D', 'title'));