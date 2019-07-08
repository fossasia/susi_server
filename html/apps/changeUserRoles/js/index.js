

(function ($, window, document, undefined) {

  'use strict';

  var $html = $('html');

  $html.on('click.ui.dropdown', '.js-dropdown', function (e) {
    e.preventDefault();
    $(this).toggleClass('is-open');
  });

  $html.on('click.ui.dropdown', '.js-dropdown [data-dropdown-value]', function (e) {
    e.preventDefault();
    var item = $(this);
    var role = item.data("dropdown-value");
    var email = item.closest('.c-dropdown').data('email').trim();
    var userRole = item.text();
    var $dropdown = item.parents('.js-dropdown');
    $dropdown.find('.js-dropdown__input').val(item.data('dropdown-value'));
    $dropdown.find('.js-dropdown__current').text(item.text());

    var endPoint = "/aaa/changeRoles"; // the endpoint to post data
    // console.log(item.parent().text());
    console.log(item.parent().text());
    item.parent().text(item.text());


    $.ajax({
      type: "POST",
      url: endPoint,
      data: "user=" + email + "&role=" + role,
      processData: false,
      contentType: 'application/x-www-form-urlencoded',
      complete: function (xhr) {
        if (xhr.status == 200) {
          alert("Role changed successfully");
        } else {
          alert(xhr.statusText);
        }
      },
    });

  });

  $html.on('click.ui.dropdown', function (e) {
    var $target = $(e.target);
    if (!$target.parents().hasClass('js-dropdown')) {
      $('.js-dropdown').removeClass('is-open');
    }
  });

})(jQuery, window, document);

