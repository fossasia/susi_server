$(document).ready(function()
{
	$.ajax(	"/api/login.json", {
	    data: { checkLogin: true },
		dataType: "json",
		success: function (response) {
			if(response.loggedIn){
				$("#status-box").text(response.message);
				$("#status-box").addClass("error");
				$("#loginForm").addClass("hidden");
				$("#logoutForm").removeClass("hidden");
			}
		},
		error: function (xhr, ajaxOptions, thrownError) {
			$("#status-box").text(thrownError);
			$("#status-box").addClass("error");
		},
	});

    function setRemember(){
        if($("#remember").is(":checked")){
            $("#remember_hidden").val("cookie");
        }
        else{
            $("#remember_hidden").val("session");
        }
    }
    $("#remember").click(function(){setRemember();});
    setRemember();

	var optionsLogin = {
        url:        "/api/login.json",
        type:       "get",
        dataType:   "json",
        success(response) {
            window.location = "/apps/applist/index.html";
        },
        error(xhr, ajaxOptions, thrownError) {
            $("#status-box").text(thrownError);
            $("#status-box").addClass("error");
        }
    };

    $("#loginForm").submit(function() {
        $(this).ajaxSubmit(optionsLogin);
        return false;
    });

    var optionsLogout = {
        url:        "/api/login.json",
        type:       "get",
        dataType:   "json",
        success(response) {
            $("#loginForm").removeClass("hidden");
            $("#logoutForm").addClass("hidden");
            $("#status-box").text("");
            $("#status-box").removeClass();
        },
        error(xhr, ajaxOptions, thrownError) {
            $("#status-box").text(thrownError);
            $("#status-box").addClass("error");
        }
    };

    $("#logoutForm").submit(function() {
        $(this).ajaxSubmit(optionsLogout);
        return false;
    });
});
