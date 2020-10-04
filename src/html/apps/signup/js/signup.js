$(document).ready(function()
{
	// get password parameters
	$.ajax(	"/aaa/signup.json", {
		data: { getParameters: true },
		dataType: "json",
		success: function (response) {
			var regex = response.regex;
			var regexTooltip = response.regexTooltip;

			$("#pass").attr("pattern", regex);
			$("#passconfirm").attr("pattern", regex);

			$("#pass").attr("title", regexTooltip);
			$("#pass").tooltip({"trigger":"focus", "placement": "left", "title": regexTooltip});

			$("#status-box").text("");
			$("#status-box").removeClass("error");
			$("#email").removeClass("hidden");
			$("#pass").removeClass("hidden");
			$("#confirmpass").removeClass("hidden");
			$("#signup").removeClass("hidden");
		},
		error: function (xhr, ajaxOptions, thrownError) {
			$("#status-box").text(thrownError);
			$("#status-box").addClass("error");
			$("#email").addClass("hidden");
			$("#pass").addClass("hidden");
			$("#confirmpass").addClass("hidden");
			$("#signup").addClass("hidden");
		},
	});

	function resetFields(){
		$("#status-box").text("");
		$("#status-box").removeClass();
		$("#email").val("");
		$("#email").removeClass();
		$("#pass").val("");
		$("#pass").removeClass();
		$("#confirmpass").val("");
		$("#confirmpass").removeClass();
		$("#valid").text("");
		$("#valid").removeClass();
		$("#matching").text("");
		$("#matching").removeClass();
		$("#passtrength").text("");
		$("#passtrength").removeClass();
	}

	function checkPasswordSameAsEmail(){
		if($("#email").val() === $("#pass").val()){
			$("#pass")[0].setCustomValidity("Password must not be equal to the email address");
			return false;
		} else {
			$("#pass")[0].setCustomValidity("");
			return true;
		}
	}

	function checkMatching(){
		if(!$("#pass")[0].checkValidity() || $("#pass").val() !== $("#confirmpass").val()) {
			$("#confirmpass")[0].setCustomValidity("Passwords don't Match");
			return false;
		} else {
			$("#confirmpass")[0].setCustomValidity("");
			return true;
		}
	}

	function checkConfirmPass(){
		$("#confirmpass").removeClass();
		$("#matching").text("");

		if(!$("#confirmpass").val()){return;}

		checkMatching();
		if($("#confirmpass")[0].checkValidity()){
			$("#confirmpass").addClass("success");
			$("#matching").text("Passwords match!");
		}
	}

	function setStrengthlvl(){

        var pass = $("#pass").val();

        var strength = 0;
        $("#passtrength").removeClass();
        if(pass.length === 0){
            $("#passtrength").text("");
            return false;
        }
        if(!$("#pass")[0].checkValidity()){
           $("#passtrength").addClass("error");
           $("#passtrength").text("Insufficient password");
           return false;
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

        $("#passtrength").removeClass();
        if (strength < 2 ){
            $("#passtrength").css("color", "orange"); // color changes
            $("#passtrength").text("Weak");
        }
        else if (strength >= 2 && strength < 4){
            $("#passtrength").css("color", "LightGreen");
            $("#passtrength").text("Good");
        }
        else{
            $("#passtrength").css("color", "GreenYellow");
            $("#passtrength").text("Strong");
        }
        return true;
    }

	$("#email").keyup(function(){
		if($("#email")[0].checkValidity()){
			$("#email").addClass("success");
			$("#valid").text("Email valid!");
			$("#valid").addClass("success");
		}
		else{
			$("#email").removeClass();
			$("#valid").text("");
		}
	});

	$("#pass").focus(function(){
		$("#valid").removeClass();
		if($("#email")[0].checkValidity()){
			$("#email").addClass("success");
			$("#valid").text("Email valid!");
			$("#valid").addClass("success");
		}
		else{
			$("#email").addClass("error");
			$("#valid").text("Invalid email!");
			$("#valid").addClass("error");
		}
	})

	$("#pass").keyup(function(){
		checkPasswordSameAsEmail();

		$("#pass").removeClass();
		if(setStrengthlvl()){
			$("#pass").addClass("success");
		}

		checkConfirmPass();
	});

	$("#confirmpass").keyup(function(){checkConfirmPass();});

	var options = {
        url:        "/aaa/signup.json",
        type:       "get",
        dataType:   "json",
        success(response) {
            resetFields();
            $("#status-box").text(response.message);
        },
        error(xhr, ajaxOptions, thrownError) {
            $("#status-box").text(thrownError);
            $("#status-box").addClass("error");
        }
    };

	$("#form").submit(function() {
		checkPasswordSameAsEmail();
		checkMatching();
		$(this).ajaxSubmit(options);
		return false;
	});
});
