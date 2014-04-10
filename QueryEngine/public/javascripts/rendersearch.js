$(document).ready(function() {

  var ResultPaperCollection = Backbone.Collection.extend({


  });

  var resultPaperCollection = new ResultPaperCollection();


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
    },

    addOne: function (paper) {
      var view = new SinglePaperView({ model: paper });
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
    },

    clickOnBtn: function(event) {
      console.log('btn clicked');
      console.log(event);
    },

    ajaxPostRequest: function(data, url, callback) {

      $.ajax({
        url: url,
        type: "POST",
        dataType: "json",
        headers: {'Content-Type':'application/json'},
        data: JSON.stringify(data),
        success: function(result) {
          callback(result);
        },
        error: function (xhr, status, err) {
          console.log(xhr);
        }
      });
    },

    dismissAlert: function(event) {
      console.log(event);
      $('.alert-dismissable').fadeOut();
    },

    simpleSearch: function(event) {
      var titleIn = $.trim($('#simple-search-title').val());
      var authorIn = $.trim($('#simple-search-author').val());

      if (titleIn || authorIn) {
        var data = {title:titleIn, author:authorIn};
        this.ajaxPostRequest(data, '/simpleSearch', function(result){
            resultPaperCollection.reset();
            resultPaperCollection.set(result.data);
        });

      } else {
        this.alertBox.fadeIn('fast');
      }
    },

    collaborationSearch: function(event) {
      var authorFromIn = $.trim($('#collaboration-search-author1').val());
      var authorToIn = $.trim($('#collaboration-search-author2').val());
      if(authorFromIn && authorToIn) {
        var data = {authorFrom:authorFromIn, authorTo:authorToIn};
        console.log(data);
        this.ajaxPostRequest(data, '/collaborationDistance', function(result){
            resultPaperCollection.reset();
            console.log(result);
        });
      } else {
        this.alertBox.fadeIn('fast');
      }
    },

    similarPaperSearch: function(event) {
      var titleIn = $.trim($('#similar-search-paper').val());
      if(titleIn) {
        var data = {title:titleIn};
        this.ajaxPostRequest(data, '/similarCitationPaper', function(result){
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