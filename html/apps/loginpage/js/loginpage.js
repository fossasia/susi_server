$(document).ready(function()
{
	var emailerr = false, passerr = false, checked = $('#remember').prop("checked"), logout = false;

	$.ajax(	"/api/login.json", {
	    data: { checkLogin: true },
		dataType: 'json',
		success: function (response) {
			if(response.loggedIn){
				$('#status-box').text(response.message);
				$('#status-box').addClass("error");
				$('#login').text("Logout");
				$('#pass').addClass("hidden");
				$('#email').addClass("hidden");
				$('#remember').addClass("hidden");
				$('#rememberme').addClass("hidden");
				$('#signup').addClass("hidden");
				$('#forgot-password').addClass("hidden");
				logout = true;
			}
		},
		error: function (xhr, ajaxOptions, thrownError) {
			$('#status-box').text(thrownError);
			$('#status-box').addClass("error");
		},
	});

	$('#email').keyup(function(event){
        if(event.keyCode == 13){
            $("#login").click();
        }
    });

    $('#pass').keyup(function(event){
        if(event.keyCode == 13){
            $("#login").click();
        }
    });

	$('#pass').focus(function(){
		checkEmpty();
	})

	$('#remember').click(function(){
	    checked = $(this).prop("checked");
	});

	$('#login').click(function(){
		if(logout){
			$.ajax(	"/api/login.json", {
				data: { logout: true },
				dataType: 'json',
				success: function (response) {
					$('#login').text("Login");
					$('#email').removeClass();
					$('#pass').removeClass();
					$('#remember').removeClass();
					$('#rememberme').removeClass();
					$('#signup').removeClass();
					$('#forgot-password').removeClass();
					$('#status-box').text("");
					$('#status-box').removeClass();
					logout = false;
				},
				error: function (xhr, ajaxOptions, thrownError) {
					$('#status-box').text(thrownError);
					$('#status-box').addClass("error");
				},
			});
			return;
		}
		
		checkEmpty();
		var total = passerr || emailerr;
		if(!total){
			var mail = $('#email').val();
			var pwd = $('#pass').val();
			
			$.ajax(	"/api/login.json", {
				data: { login: mail, password: pwd, type: checked ? "cookie" : "session" },
				dataType: 'json',
				success: function (response) {
					window.location = '/apps/applist/index.html';
				},
				error: function (xhr, ajaxOptions, thrownError) {
					$('#status-box').text(thrownError);
					$('#status-box').addClass("error");
				},
			});
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
