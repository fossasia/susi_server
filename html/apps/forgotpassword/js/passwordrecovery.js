$(document).ready(function()
{
    var emailerr = false, session = true;

    $('#submit').click(function(){
        checkEmpty();
        if(emailerr){
            alert("Please fill email");
        } else{
            var mail = encodeURIComponent($('#email').val());
            alert("Email: " + mail + "\nSuccess, write post function");
            //post function
        }
    });

    function checkEmpty(){
        var emailval = $('#email').val();
        if(!emailval && !($('#email').is(":focus"))){
            $('#emailfield').text("Required field!");
            $('#email').removeClass();
            $('#emailfield').removeClass();
            $('#email').addClass("error");
            $('#emailfield').addClass("error");
            emailerr = true;
        } else {
            emailerr = false;
            $('#email').removeClass();
            $('#emailfield').removeClass();
            $('#emailfield').text("");
        }
    }
});