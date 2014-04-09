
$(document).ready(function() {

    var countries = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        prefetch: {
            url: 'authornames.json',
            filter: function(list) {
                return $.map(list, function(country) { return { name: country }; });
            }
        }
    });
   
    countries.initialize();

    $('#prefetch .typeahead').typeahead(null, {
        name: 'countries',
        displayKey: 'name',
    
        source: countries.ttAdapter()
    });

    var paperTitles = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        prefetch: {
            url: 'papertitles.json',
            filter: function(list) {
                return $.map(list, function(title) { return { name: title }; });
            }
        }
    });
   
    paperTitles.initialize();

    $('#prefetchpaper .typeahead').typeahead(null, {
        name: 'paperTitles',
        displayKey: 'name',
        source: paperTitles.ttAdapter()
    });
});