$(document).ready(function()
{
    var self = $('#self').prop("checked");
    self ? $('#email').addClass("hidden") : $('#email').removeClass("hidden");
    var create = $('#create').prop("checked");
    create ? $('#key').addClass("hidden") : $('#key').removeClass("hidden");
    create ? $('#typeSelect').addClass("hidden") : $('#typeSelect').removeClass("hidden");
    create ? $('#type_label').addClass("hidden") : $('#type_label').removeClass("hidden");
    create ? $('#keysizeSelect').removeClass("hidden") : $('#keysizeSelect').addClass("hidden");
    create ? $('#keysize_label').removeClass("hidden") : $('#keysize_label').addClass("hidden");

    var algorithms;

	// get password parameters
	var regex;
    $.ajax(	"/aaa/pubkey_registration.json", {
        data: { getParameters: true },
        dataType: 'json',
        success: function (response) {
            if(response.self){
                $('#status-box').text("");
                $('#status-box').removeClass("error");
                self ? $('#email').addClass("hidden") : $('#email').removeClass("hidden");
                create ? $('#key').addClass("hidden") : $('#key').removeClass("hidden");
                $('#register').removeClass("hidden");
                $('#options_self').removeClass("hidden");
                $('#options_create').removeClass("hidden");
                $('#options_general').removeClass("hidden");
                create ? $('#typeSelect').addClass("hidden") : $('#typeSelect').removeClass("hidden");
                create ? $('#type_label').addClass("hidden") : $('#type_label').removeClass("hidden");
                create ? $('#keysizeSelect').removeClass("hidden") : $('#keysizeSelect').addClass("hidden");
                create ? $('#keysize_label').removeClass("hidden") : $('#keysize_label').addClass("hidden");

                $.each(response.formats, function (i, format) {
                    $('#typeSelect').append($('<option>', {
                        value : format,
                        text : format
                    }));
                });

                algorithms = response.algorithms;
                var keySizeSet = false;
                $.each(algorithms, function (algorithm, algorithmObject) {
                    $('#algorithmSelect').append($('<option>', {
                        value : algorithm,
                        text : algorithm
                    }));
                    if(!keySizeSet){
                        $.each(algorithmObject.sizes, function (i, keySize) {
                            $('#keysizeSelect').append($('<option>', {
                                value : keySize,
                                text : keySize
                            }));
                        });
                        $('#keysizeSelect').val(algorithmObject.defaultSize);
                        keyLengthSet = true;
                    }
                });
            }
            else{
                setError("You're not allowed to register a public key");
            }
        },
        error: function (xhr, ajaxOptions, thrownError) {
            setError(thrownError);
       },
    });

    function setError(text){
        $('#status-box').text(text);
        $('#status-box').addClass("error");
        $('#email').addClass("hidden");
        $('#key').addClass("hidden");
        $('#register').addClass("hidden");
        $('#options_self').addClass("hidden");
        $('#options_create').addClass("hidden");
        $('#options_general').addClass("hidden");
    }

    $('#self').click(function(){
        self = $(this).prop("checked");
        self ? $('#email').addClass("hidden") : $('#email').removeClass("hidden");
    });

    $('#create').click(function(){
        create = $(this).prop("checked");
        create ? $('#key').addClass("hidden") : $('#key').removeClass("hidden");
        create ? $('#typeSelect').addClass("hidden") : $('#typeSelect').removeClass("hidden");
        create ? $('#type_label').addClass("hidden") : $('#type_label').removeClass("hidden");
        create ? $('#keysizeSelect').removeClass("hidden") : $('#keysizeSelect').addClass("hidden");
        create ? $('#keysize_label').removeClass("hidden") : $('#keysize_label').addClass("hidden");
    });

    $('#algorithmSelect').change(function(){
        algorithm = algorithmSelect.value;
        console.log(algorithm);
        console.log("TODO: Implement change of key-sizes");
    });

    $('#register').click(function(){
        if(requiredFieldsSet()){
            var mail = encodeURIComponent($('#email').val());
            var key = encodeURIComponent($('#key').val());

            var data = {};
            data["algorithm"] = algorithmSelect.value;
            if(!self) data["id"] = mail;
            if(create){
                data["create"] = true;
                data["key-size"] = keysizeSelect.value;
            }
            else{
                data["register"] = key;
                data["type"] = typeSelect.value;
            }
            //console.log(data);

            $.ajax(	"/aaa/pubkey_registration.json", {
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
