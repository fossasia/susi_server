$(document).ready(function()
{
	var emailerr = false, passerr = false, checked = false, session = true, logout = false;

	$.ajax(	"/api/login.json", {
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
			var mail = encodeURIComponent($('#email').val());
			var pwd = encodeURIComponent($('#pass').val());
			
			$.ajax(	"/api/login.json", {
				data: { login: mail, password: pwd, request_cookie: checked, request_session: session },
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
