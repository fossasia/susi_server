$(document).ready(function()
{
	$.ajax(	"/aaa/account-permissions.json", {
		dataType: "json",
		success (response) {
		    $("#permissions").removeClass("hidden");
			$("#userName").children().first().append(response.userName);
			jQuery.each(response.userSpecificPermissions, function(service, obj){
				$("#userSpecificPermissions").append(service);
			});
			$("#userRole").children().first().append(response.userRole);
			jQuery.each(response.userRoleSpecificPermissions, function(service, obj){
				$("#userRoleSpecificPermissions").append(service);
			});
			$("#ParentUserRole").children().first().append(response.userRole);
		},
		error (xhr, ajaxOptions, thrownError) {
			$("#notLoggedIn").removeClass("hidden");
			return;
		}
	});

	$.ajax(	"/aaa/account-permissions.json", {
		data: { getServiceList: true },
		dataType: "json",
		success (response) {
			jQuery.each(response.serviceList, function(i, service){
				$("#serviceList").append("<div class='service' id='" + service + "'><span class='serviceTitle'>" + service.substr(service.lastIndexOf(".")+1) + "</span><div class='serviceContent' valueSet=false></div></div><br>");
			});
		}
	});

	$(document).on("click",".serviceTitle",function(){
		var obj = $(this).parent();
		var child = obj.children("div").first();

		if(child.attr("valueSet") === "false"){
			child.hide();
			$.ajax(	"/aaa/account-permissions.json", {
				data: { getServicePermissions: obj.attr("id") },
				dataType: "json",
				success (response) {
					if(Object.keys(response.servicePermissions).length > 0){
						child.append("<ul></ul>");
						jQuery.each(response.servicePermissions, function(i, permission){
							child.find("ul").append("<li>" + i + ": " + permission + "</li>");
						});
					}
					else{
						child.append("No permissions specified");
					}
					child.attr("valueSet", "true");
				}
			});
		}
		child.toggle();
	});
});