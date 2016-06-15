var x = new Date();
var currentTimeZoneOffset = x.getTimezoneOffset();
var tzOffset = document.getElementsByName('timezoneoffset');
tzOffset[0].value = currentTimeZoneOffset;
tzOffset[0].disabled = true;

// Input fields for data as POST explicitly hidden
$('div#inputFields').hide();

// jQuery radio button handler to show and hide required fields
// depending on the API Chosen.
$(document).ready(function(){
    $('input[type=radio]').click(function(){
        if (this.value == 'search.json' || this.value == 'true' || this.value == 'false') {
        	constructQuery();
        	$("#searchOptions").show();
        	$('div#inputFields').hide();
        }
        else if (this.value == 'xml2json.json') {
        	constructQuery();
        	$('div#inputFields').show();
        	$("#searchOptions").hide();
        }
        else {
        	constructQuery();
        	$("#searchOptions").hide();
        	$('div#inputFields').hide();
        }
    });

    $('input[type=checkbox]').click(function() {
    	var aggregationsCheckboxObject = document.getElementsByName('aggregations');
    	var aggregations = getCheckedCheckboxValue(aggregationsCheckboxObject);
    	constructQuery();
    });

    $('#query').bind('input', function() {
    	constructQuery();
    });

    $('#from').bind('input', function() {
    	constructQuery();
    });

    $('#since').bind('input', function() {
    	constructQuery();
    });

    $('#until').bind('input', function() {
    	constructQuery();
    });

    $('#near').bind('input', function() {
    	constructQuery();
    });

    $('#source').on('change', function() {
    	constructQuery();
    });

    $('#count').bind('input', function() {
    	constructQuery();
    });

    $('#limit').bind('input', function() {
    	constructQuery();
    });

    $('#inputFieldArea').bind('input', function() {
    	constructQuery();
    });
});

// Pass the radioObjects and it checks and 
// returns the value of the radio object
function getCheckedRadioValue(radioObj) {
	if(!radioObj)
		return "";
	var radioLength = radioObj.length;
	if(radioLength == undefined)
		if(radioObj.checked)
			return radioObj.value;
		else
			return "";
	for(var i = 0; i < radioLength; i++) {
		if(radioObj[i].checked) {
			return radioObj[i].value;
		}
	}
	return "";
}

// Pass the checkBoxObjects and it returns
// the values of the objects that are checked in
// On the checkbox

function getCheckedCheckboxValue(checkBoxObj) {
	if (!checkBoxObj)
		return "";
	var checkBoxesLength = checkBoxObj.length;
	var checkedItems = [];
	for (var i=0; i < checkBoxesLength; i++) {
		if (checkBoxObj[i].checked == true) {
			checkedItems.push(checkBoxObj[i].value);
		}
	}
	return checkedItems;
}

function queryLoklak() {
	// Generates the XHR and display's content
	queryString = $('#queryGenerated').val();
	$.getJSON(queryString, function (data, status) {
		$('#queryResult').val(JSON.stringify(data, null, 2));
	});
}

function constructQuery() {
	var APIRadioObject = document.getElementsByName('api');
	var selectedAPI = getCheckedRadioValue(APIRadioObject);
	// selectedAPI Contains the type of query to be made
	// status.json, search.json, peers.json etc..,
	var minifiedRadioObject = document.getElementsByName('minified');
	var minifiedType = getCheckedRadioValue(minifiedRadioObject);
	// Minified type contains value True/False
	var aggregationsCheckboxObject = document.getElementsByName('aggregations');
	var aggregations = getCheckedCheckboxValue(aggregationsCheckboxObject);
	// Data input for data=
	var ipField = $('#inputFieldArea').val();

	if (selectedAPI != 'search.json') {
		serviceURL = $(location).attr('href').split('apps/LQL/')[0];
		var constructedURL = serviceURL;
		constructedURL += 'api/' + selectedAPI;
		if (selectedAPI == 'xml2json.json') {
			if (ipField != '') {
				constructedURL += '?data='+ipField;
			}
		}
		$('#queryGenerated').val(constructedURL);
	}
	else {
		var query = $('#query').val();
		var timeZoneOffset = $('#timezoneoffset').val();
		var serviceURL = $(location).attr('href').split('apps/LQL/')[0];
		var constructedURL = serviceURL;
		var from = $('#from').val();
		var since = $('#since').val();
		var until = $('#until').val();
		var near = $('#near').val();
		var source = $('#source').val();
		var count = $('#count').val();
		var limit = $('#limit').val();
		var minified = minifiedType;

		constructedURL += 'api/'+selectedAPI+'?';
		if (query != '') {
			// Query q construction
			constructedURL += 'timezoneOffset='+timeZoneOffset;
			if (from != '') {
				query += ' from:'+from;
			}
			if (since != '') {
				query += ' since:'+since;
			}
			if (until != '') {
				query += ' until:'+until;
			}
			if (near != '') {
				query += ' near:'+near;
			}
			constructedURL += '&q='+query;

			// Aggregations fields
			if (aggregations != '') {
				var aggQuery = aggregations.join(",");
				constructedURL += '&fields=' + aggQuery;
			}

			// Source type
			if (source != '') {
				constructedURL += '&source=' + source;
			}

			// Count
			if (count != '') {
				constructedURL += '&count='+count;
			}

			// Limit
			if (limit != '') {
				constructedURL += '&limit='+limit;
			}

			if (minified != '') {
				constructedURL += '&minified='+minified;
			}
		}
		$('#queryGenerated').val(constructedURL);
	}
}
