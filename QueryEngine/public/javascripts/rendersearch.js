$(document).ready(function() {

  var ResultPaperCollection = Backbone.Collection.extend({

  });

  var ResultAuthorCollection = Backbone.Collection.extend({

  });

  var resultPaperCollection = new ResultPaperCollection();
  var resultAuthorCollection = new ResultAuthorCollection();


  var SingleAuthorView = Backbone.View.extend({
    tagName: 'li',

    render: function() {
      var template = _.template($("#single-author-template").html(), {model: this.model.toJSON()});
      return $(this.el).html(template);
    },

  });


  var SinglePaperView = Backbone.View.extend({

    tagName: 'li',

    render: function(){
      var template = _.template($("#single-paper-template").html(), {model: this.model.toJSON()});
      return $(this.el).html(template);
    },

    events: {
      'click .collapse-btn': 'collapse',
    },

    collapse: function(event) {
      event.preventDefault();
      var elem = this.$el.find('.collapse');
      elem.collapse('toggle');
      if(elem.is(':visible')) {
        event.currentTarget.setAttribute('class', 'collapse-btn glyphicon glyphicon-chevron-down')
      } else {
        event.currentTarget.setAttribute('class', 'collapse-btn glyphicon glyphicon-chevron-right')
      }
    },
  });

  var ResultListView = Backbone.View.extend({

    el: '#result-list',

    initialize: function() {
      this.listenTo(resultPaperCollection, 'reset', this.clear);
      this.listenTo(resultPaperCollection, 'add', this.addOne);

      this.listenTo(resultAuthorCollection, 'reset', this.clear);
      this.listenTo(resultAuthorCollection, 'add', this.addOneAuthor);
    },

    addOne: function (paper) {
      var view = new SinglePaperView({ model: paper });
      this.$el.append(view.render());
    },

    addOneAuthor: function(author) {
      var view = new SingleAuthorView({ model: author });
      this.$el.append(view.render());
    },

    clear: function() {
      this.$el.empty();
    }
  });

  var IndexView = Backbone.View.extend({

    el: 'body',

    events: {
      'click #simple-search-btn': 'simpleSearch',
      'click #collaboration-search-btn': 'collaborationSearch',
      'click #similar-paper-search-btn': 'similarPaperSearch',
      'click .dismiss-btn': 'dismissAlert',
    },

    initialize: function(options) {
      this.alertBox = $('.alert-dismissable');
      this.alertBox.hide();
      $('#distance').hide();
      $('#path').hide();
    },

    clear: function() {
        this.dismissAlert();
        $('#distance').hide();
        $('#path').hide();
    },

    ajaxPostRequest: function(data, url, callback) {
      this.showLoadingStatus();
      $.ajax({
        context: this,
        url: url,
        type: "POST",
        dataType: "json",
        headers: {'Content-Type':'application/json'},
        data: JSON.stringify(data),
        success: function(result) {
          callback(result);
          this.hideLoadingStatus();
        },
        error: function (xhr, status, err) {
          console.log(xhr);
        }
      });
    },

    dismissAlert: function(event) {
      $('.alert-dismissable').fadeOut();
      this.alertBox.find('p').html('<strong>Invalid input!</strong> Please fill in all required fields.');
    },

    showLoadingStatus: function() {
      var loadingDialog = document.getElementById('loading-dialog');
      loadingDialog.style.display = 'block';
      var loadingOverlay = document.getElementById('loading-overlay');
      loadingOverlay.style.display = 'block';
    },

    hideLoadingStatus: function() {
      var loadingDialog = document.getElementById('loading-dialog');
      loadingDialog.style.display = 'none';
      var loadingOverlay = document.getElementById('loading-overlay');
      loadingOverlay.style.display = 'none';
    },

    simpleSearch: function(event) {
      this.clear();
      var titleIn = $.trim($('#simple-search-title').val());
      var authorIn = $.trim($('#simple-search-author').val());
      if (titleIn || authorIn) {
        var data = {title:titleIn, author:authorIn};
        this.ajaxPostRequest(data, '/simpleSearch', function(result){
            resultPaperCollection.reset();
            resultAuthorCollection.reset();
            resultPaperCollection.set(result.data);
        });

      } else {
        this.alertBox.fadeIn('fast');
      }
    },

    collaborationSearch: function(event) {
        this.clear();
        $('#distance').hide();
        var authorFromIn = $.trim($('#collaboration-search-author1').val());
        var authorToIn = $.trim($('#collaboration-search-author2').val());
        var alertBox = this.alertBox;
      if(authorFromIn && authorToIn) {
        var data = {authorFrom:authorFromIn, authorTo:authorToIn};
        
        this.ajaxPostRequest(data, '/collaborationDistance', function(result){
            resultAuthorCollection.reset();
            resultPaperCollection.reset();
            console.log('result', result);
            if (result.error) {
                alertBox.find('p').html(result.error);
                alertBox.fadeIn('fast');
            } else {
                $('#distance').html('Collaboration distance: ' + result.distance.toString());
                $('#distance').show();
                $('#path').show();
                resultAuthorCollection.set(result.path);
            }
        });
      } else {
        alertBox.fadeIn('fast');
      }
    },

    similarPaperSearch: function(event) {
        this.clear();
      var titleIn = $.trim($('#similar-search-paper').val());
      if(titleIn) {
        var data = {title:titleIn};
        this.ajaxPostRequest(data, '/similarCitationPaper', function(result){
            resultAuthorCollection.reset();
            resultPaperCollection.reset();
            resultPaperCollection.set(result.data.papers);
        });
      } else {
        this.alertBox.fadeIn('fast');
      }
    }
  });

var indexView = new IndexView();
var resultListView = new ResultListView();

});