$(document).ready(function()
{
  var emailerr = false, passerr = false, confirmerr = false;

	// get password parameters
	var regex;
	$.ajax(	"/api/signup.json", {
       data: { getParameters: true },
       dataType: 'json',
       success: function (response) {
        regex = response.regex;
        var regexTooltip = response.regexTooltip;
        $('#pass').tooltip({'trigger':'focus', 'placement': 'left', 'title': regexTooltip});
    },
    error: function (xhr, ajaxOptions, thrownError) {
       $('#status-box').text(thrownError);
       $('#status-box').addClass("error");
       $('#email').prop( "disabled", true );
       $('#pass').prop( "disabled", true );
       $('#confirmpass').prop( "disabled", true );
       $('#signup').prop( "disabled", true );
   },
});

    $('#pass').keyup(function(){
        $('#passtrength').text(strengthlvl($('#pass').val()));
    });

    $('#pass').focus(function(){
        var emailval = $('#email').val();
        var regex = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(emailval){
            $('#email').removeClass();
            $('#valid').removeClass();
            if(regex.test(emailval)){
                $('#valid').text("Email valid!");
                $('#email').addClass("success");
                $('#valid').addClass("success");
                emailerr = false;
            }
            else{
                $('#valid').text("Invalid email!");
                $('#email').addClass("error");
                $('#valid').addClass("error");
                emailerr = true;
            }
        }
        else{
            $('#valid').text("Required field!");
            $('#email').addClass("error");
            $('#valid').addClass("error");
            emailerr = true;
        }
    })

    $('#pass').keyup(function(){
        $('#confirmpass').removeClass();
        $('#matching').removeClass();
        if($('#confirmpass').val() && $(this).val()!=$('#confirmpass').val()){
            $('#confirmpass').removeClass();
            $('#confirmpass').addClass("error");
            $('#matching').text("");
            confirmerr = true;
        } else if ($('#confirmpass').val() && $(this).val()==$('#confirmpass').val() && $(this).val().length >=6) {
            $('#confirmpass').addClass("success");
            $('#matching').addClass("success");
            $('#matching').text("Passwords match!");
            confirmerr = false;
        }
    })

    $('#confirmpass').focus(function(){
        checkEmpty();
    })

    $('#confirmpass').keyup(function(){
        var pass = $('#pass').val();
        var confirmpass = $(this).val();
        if(confirmpass){
            $(this).removeClass();
            $('#matching').removeClass();
            if(confirmpass == pass && pass.length >= 6){
                $(this).addClass("success");
                $('#matching').addClass("success");
                $('#matching').text("Passwords match!");
                confirmerr = false;
            } else {
                $(this).addClass("error");
                $('#matching').text("");
                confirmerr = true;
            }
        } else {
            $(this).removeClass();
            $('#matching').text("");
        }
    });

    $('#signup').click(function(){
        checkEmpty();
        var total = passerr || confirmerr || emailerr;
        if(!total){
            var mail = $('#email').val();
            var pwd = $('#pass').val();

            $.ajax(	"/api/signup.json", {
             data: { signup: mail, password: pwd },
             dataType: 'json',
             success: function (response) {
                 resetFields();
                 $('#status-box').text(response.message);
             },
             error: function (xhr, ajaxOptions, thrownError) {
              $('#status-box').text(thrownError);
              $('#status-box').addClass("error");
          },
      });
        }
    });

    function resetFields(){
        $('#status-box').text("");
        $('#status-box').removeClass();
        $('#email').val("");
        $('#email').removeClass();
        $('#pass').val("");
        $('#pass').removeClass();
        $('#confirmpass').val("");
        $('#confirmpass').removeClass();
        $('#valid').text("");
        $('#valid').removeClass();
        $('#matching').text("");
        $('#matching').removeClass();
        $('#passtrength').text("");
        $('#passtrength').removeClass();
    }

    function strengthlvl(pass){

        var strength = 0;
        $('#passtrength').removeClass();
        if(pass.length == 0){
            return "";
        }
        if(!pass.match(regex)){
           $('#passtrength').addClass("error");
           passerr = true;
           return "Insufficient password";
       }

        if (pass.length >=7) { //sufficient length
            strength += 1;
        }
        if (pass.match(/([a-z].*[A-Z])|([A-Z].*[a-z])/)){ //both uppercase and lowercase
            strength += 1;
        }
        if (pass.match(/([a-zA-Z])/) && pass.match(/([0-9])/)){ //both letters and nums
            strength += 1;
        }
        if (pass.match(/([^a-zA-Z0-9])/)){ //alphanumeric
            strength += 1;
        }

        if (pass.match(/(.*[^a-zA-Z0-9].*[^a-zA-Z0-9])/)){ //more than two alphanumeric chars
            strength += 1;
        }

        passerr = false;
        $('#pass').removeClass();
        $('#pass').addClass("success");
        $('#passtrength').removeClass();
        if (strength < 2 )
        {
            $('#passtrength').css('color', 'orange'); // color changes
            return "Weak";
        }
        else if (strength >= 2 && strength < 4)
        {
            $('#passtrength').css('color', 'LightGreen');
            return "Good";
        }
        else
        {
            $('#passtrength').css('color', 'GreenYellow');
            return "Strong";
        }

    }

    function checkEmpty(){
        var emailval = $('#email').val();
        var passval = $('#pass').val();
        var confirmval = $('#confirmpass').val();
        if(!emailval && !($('#email').is(":focus"))){
            $('#valid').text("Required field!");
            $('#email').removeClass();
            $('#valid').removeClass();
            $('#email').addClass("error");
            $('#valid').addClass("error");
            emailerr = true;
        }
        if(!passval && !($('#pass').is(":focus"))){
            $('#passtrength').text("Required field!");
            $('#pass').removeClass();
            $('#passtrength').removeClass();
            $('#pass').addClass("error");
            $('#passtrength').addClass("error");
            passerr = true;
        }
        if(!confirmval && !($('#confirmpass').is(":focus"))){
            $('#matching').text("Required field!");
            $('#confirmpass').removeClass();
            $('#matching').removeClass();
            $('#confirmpass').addClass("error");
            $('#matching').addClass("error");
            confirmerr = true;
        }
    }
});
