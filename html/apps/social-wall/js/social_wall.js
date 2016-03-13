$(document).keypress(function(event){

    var keycode = (event.keyCode ? event.keyCode : event.which);
    if(keycode == '13'){
        $('#search-but').click();   
    }

});

function twitterLinks(text)
{
  var base_url = 'http://twitter.com/';   // identica: 'http://identi.ca/'
  var hashtag_part = 'search?q=%23';        // identica: 'tag/'

  // convert protocol-less URLs into links        
  text = text.replace(
      /(:\/\/|>)?\b(([-a-z0-9]+\.)+[a-z]{2,5}(\/[-a-z0-9!#()\/?&.]*[^ !#?().,])?)/gi,
      function($0, $1, $2) {
          return ($1 ? $0 : '<a href="http://' + $2 + '">' + $2 + '</a>');
      });
  // convert @mentions into follow links
  text = text.replace(
      /(:\/\/|>)?(@([_a-z0-9\-]+))/gi,
      function($0, $1, $2, $3) {
          return ($1 ? $0 : '<a href="' + base_url + $3
              + '" title="Follow ' + $3 + '" target="_blank">@' + $3
              + '</a>');
      });
  // convert #hashtags into tag search links
  text = text.replace(
      /(:\/\/[^ <]*|>)?(\#([_a-z0-9\-]+))/gi,
      function($0, $1, $2, $3) {
          return ($1 ? $0 : '<a href="' + base_url + hashtag_part + $3
              + '" title="Search tag: ' + $3 + '" target="_blank">#' + $3
              + '</a>');
      });
  return text;
}

$(function() {
  $("#query").focus();
});

$('#search-but').click(function(event){
  var query = $("#query").val();
  query = '/api/search.json?q=' + encodeURIComponent(query);

  $("#masonry").empty();
  $("#masonry").html("<div class='item-size'></div>");
  $('#loadingmessage').show();

  $.getJSON( 
    query       
  )
  .done(function(data){

    $('#loadingmessage').hide();

    var theTemplateScript = $("#feeds").html();
    var theTemplate = Handlebars.compile(theTemplateScript);
    var context = data.statuses;
    var container = document.querySelector('#masonry');

    Handlebars.registerHelper('text', function() {
      return new Handlebars.SafeString(
        twitterLinks(this.text)
      );
    });

    Handlebars.registerHelper("prettify", function(created_at) {
      return moment(new Date(created_at)).fromNow();
    });

    $.each(context, function (i, item)
    {    
      $.each(item.images, function (j, item1)
      {
        if(item1.indexOf("instagram") > -1) 
        {      
          item.images.splice(i,1);
          item.images_count--;  
        }
      });

      var theCompiledHtml = theTemplate(item);  
      $("#masonry").append(theCompiledHtml);

      var masonry = new Masonry(container, {
        columnWidth: '.item-size',
        itemSelector: '.item',
        percentPosition: true,
        gutter: 8
      });

    });

    imagesLoaded(container, function(){
      var masonry = new Masonry(container, {
        columnWidth: '.item-size',
        itemSelector: '.item',
        percentPosition: true,
        gutter: 8
      });
    })

    $(".card").on("click", function(){
      window.open($(this).attr('data-link'), '_blank');
    });

    $(".card__author-content").on("click", function(){
      window.open($(this).attr('data-author-link'), '_blank');
    });

  });
});
