$(document).ready(function()
{
    var self = $('#self').prop("checked");
    self ? $('#email').addClass("hidden") : $('#email').removeClass("hidden");
    var create = $('#create').prop("checked");
    create ? $('#key').addClass("hidden") : $('#key').removeClass("hidden");

	// get password parameters
	var regex;
    $.ajax(	"/api/pubkey_registration.json", {
        data: { getParameters: true },
        dataType: 'json',
        success: function (response) {
            if(!response.self){
                $('#status-box').text("You're not allowed to register a public key");
                $('#status-box').addClass("error");
                $('#email').prop( "disabled", true );
                $('#key').prop( "disabled", true );
                $('#register').prop( "disabled", true );
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
           $('#status-box').text(thrownError);
           $('#status-box').addClass("error");
           $('#email').prop( "disabled", true );
           $('#key').prop( "disabled", true );
           $('#register').prop( "disabled", true );
       },
    });

    $('#self').click(function(){
        self = $(this).prop("checked");
        self ? $('#email').addClass("hidden") : $('#email').removeClass("hidden");
    });

    $('#create').click(function(){
        create = $(this).prop("checked");
        create ? $('#key').addClass("hidden") : $('#key').removeClass("hidden");
    });

    $('#register').click(function(){
        if(requiredFieldsSet()){
            var mail = encodeURIComponent($('#email').val());
            var key = encodeURIComponent($('#key').val());

            var data = {};
            if(!self) data["id"] = mail;
            if(create){
                data["create"] = true;
            }
            else{
                data["register"] = key;
            }
            console.log(data);

            $.ajax(	"/api/pubkey_registration.json", {
                data: data,
                dataType: 'json',
                success: function (response) {
                    //resetFields();
                    $('#status-box').text(response.message);
                    $('#output').removeClass("hidden");
                    $('#pubkey_DER').text(response.publickey_DER_BASE64);
                    $('#pubkey_PEM').text(response.publickey_PEM);
                    $('#hash').text(response.keyhash);
                    $('#hash_URLSAVE').text(response.keyhash_urlsave);
                    if(create){
                        $('#privkey_DER').removeClass("hidden");
                        $('#privkey_DER_label').removeClass("hidden");
                        $('#privkey_DER').text(response.privatekey_DER_BASE64);
                        $('#privkey_PEM').removeClass("hidden");
                        $('#privkey_PEM_label').removeClass("hidden");
                        $('#privkey_PEM').text(response.privatekey_PEM);
                    }
                    $("#form").addClass("hidden");
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
        $('#status-box').removeClass("error");
        $('#email').val("");
        $('#email').removeClass("error");
        $('#email_label').text("");
        $('#email_label').removeClass("error");
        $('#key_label').text("");
        $('#key_label').removeClass("error");
    }

    function requiredFieldsSet(){
        var emailval = $('#email').val();
        var keyval = $('#key').val();

        var result = true;

        if(!self && !emailval && !($('#email').is(":focus"))){
            $('#valid').text("Required field!");
            $('#email').removeClass();
            $('#valid').removeClass();
            $('#email').addClass("error");
            $('#valid').addClass("error");
            result = false;
        }
        if(!create && !keyval && !($('#key').is(":focus"))){
            $('#key_label').text("Required field!");
            $('#key').removeClass();
            $('#key_label').removeClass();
            $('#key').addClass("error");
            $('#key_label').addClass("error");
            result = false;
        }

        return result;
    }
});
