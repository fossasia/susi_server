$(document).ready(function()
{
	var passerr = false, confirmerr = false, tokenerr = false;

	// get password parameters
	var regex;
	var urltoken = getParameter('token');

	$.ajax(	"/aaa/recoverpassword.json", {
		data: { getParameters: true, token: urltoken },
		dataType: 'json',
		success: function (response) {
			regex = response.regex;
			var regexTooltip = response.regexTooltip;
			$('#pass').tooltip({'trigger':'focus', 'placement': 'left', 'title': regexTooltip});
			$('#status-box').text(response.message);
			tokenerr = false;
		},
		error: function (xhr, ajaxOptions, thrownError) {
			$('#status-box').text(thrownError);
			$('#status-box').addClass("error");
			$('#pass').prop( "disabled", true );
			$('#confirmpass').prop( "disabled", true );
			$('#resetbut').prop( "disabled", true );
			tokenerr = true;
		},
	});

	$('#pass').keyup(function(){
		$('#passtrength').text(strengthlvl($('#pass').val()));
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
	});

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

	$('#resetbut').click(function(){
		checkEmpty();
		var total = passerr || confirmerr || tokenerr;
		if(!total){
			var newpass = $('#pass').val();

			$.ajax(	"/aaa/resetpassword.json", {
				data: { token: urltoken, newpass: newpass },
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
		$('#pass').val("");
		$('#pass').removeClass();
		$('#confirmpass').val("");
		$('#confirmpass').removeClass();
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
    	var passval = $('#pass').val();
    	var confirmval = $('#confirmpass').val();
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

    function getParameter(theParameter) { 
    	var params = window.location.search.substr(1).split('&');
    	
    	for (var i = 0; i < params.length; i++) {
    		var p=params[i].split('=');
    		if (p[0] == theParameter) {
    			return p[1];
    		}   
    	}
    	return null;
    }

});
