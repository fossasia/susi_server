$(document).ready(function(){
    $.ajax('/learning/rule.json?action=list', {
        dataType: "json",
        success: function(response){
            for(var key in response.projects.default) {
                $('#listRules').append('<li id='+key+'><h4>'+key+'</h4><div id="values" class='+key+'><ul><li>URL: '+response.projects.default[key].url+'</li><li>Test Name: '+response.projects.default[key].test+'</li></div><a class="btn btn-danger" href="/learning/rule.json?action=delete&name='+key+'" id="delete">Delete Rule</a></li>');
            }
       }
    });
    $('#test').click(function(){
        var url = $('#inputUrl').val();
        var testname = $('#inputTestName').val();
        var name = $('#inputName').val();
        var path = $('#inputPath').val();
        window.location = '/learning/rule.json?action=test&test='+testname+'&url='+url+'&name='+name+'&path='+path;
    });
    $('#learn').click(function(){
        var url = $('#inputUrl').val();
        var testname = $('#inputTestName').val();
        var name = $('#inputName').val();
        var path = $('#inputPath').val();
        window.location = '/learning/rule.json?action=learn&test='+testname+'&url='+url+'&name='+name+'&path='+path;
    });
    
});
