$(document).ready(function()
{
    var emailerr = false, passerr = false, checked = false, session = true;

    $('#pass').focus(function(){
        checkEmpty();
    })

    $('#remember').click(function(){
        if($(this).prop("checked")){
            checked = true;
        } else{
            checked = false;
        }
    });

    $('#login').click(function(){
        checkEmpty();
        var total = passerr || emailerr;
        if(total){
            alert("Please fill empty fields");
        } else{
            var mail = encodeURIComponent($('#email').val());
            var pwd = encodeURIComponent($('#pass').val());
            console.log(mail + "\n" + pwd + "\n" + checked + "\n" + session);
            var posting = $.post( "/api/login.json", { login: mail, password: pwd, request_cookie: checked, request_session: session }, function(data) {
                console.log(data.status);
                console.log(data.reason);
                alert(data.status + ", " + data.reason);
            }, "json" );
        }
    });

    function checkEmpty(){
        var emailval = $('#email').val();
        var passval = $('#pass').val();
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
        if(!passval && !($('#pass').is(":focus"))){
            $('#passfield').text("Required field!");
            $('#pass').removeClass();
            $('#passfield').removeClass();
            $('#pass').addClass("error");
            $('#passfield').addClass("error");
            passerr = true;
        } else {
            passerr = false;
            $('#pass').removeClass();
            $('#passfield').removeClass();
            $('#passfield').text("");
        }
    }
});