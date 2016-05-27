$(document).ready(function()
{
    $('#pass').keyup(function(){
        $('#passtrength').text(strengthlvl($('#pass').val()));
    });

    $('#confpass').keyup(function(){
        $('#matching').text(match($('#pass').val(), $('#confpass').val()))
    });

    function strengthlvl(pass){

        var strength = 0;

        if(pass.length == 0){
            $('#passtrength').removeClass();
            return "";
        }
        if(pass.length < 6){
            $('#passtrength').removeClass();
            $('#passtrength').addClass('short');
            return "Too short";
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

        if (strength < 2 )
        {
            $('#passtrength').removeClass();
            $('#passtrength').addClass('weak');
            return "Weak";           
        }
        else if (strength >= 2 && strength < 4)
        {
            $('#passtrength').removeClass();
            $('#passtrength').addClass('good');
            return "Good";       
        }
        else 
        {
            $('#passtrength').removeClass();
            $('#passtrength').addClass('strong');
            return "Strong";
        }

    }
});