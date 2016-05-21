# loklak Applications

loklak_server is the framework for messages, queries, suggestion, geocoding, accounts and more.
These services do not have a graphical front-end. Supporters may develop and submit front-ends
for loklak and send pull requests to add their front-ends to the loklak main release.
These front-ends are called 'apps' and must be placed here.

## How to create a loklak app

1. Prepare your development environment
  - clone https://github.com/loklak/loklak_server
  - run loklak yourself because all apps must use their own server

2. Create your app
  - make a subdirectory in your own ```loklak_server/html/apps/``` folder
  - add at least three files into this folder, named ```index.html```, ```app.json``` and ```screenshot.png```.
    For an easy quick-start, use and copy the app boilerplate from
    https://github.com/loklak/loklak_server/tree/master/html/apps/boilerplate
  - all libraries, css files, javascript and fonts must be either already existent
    in loklak or you must add this to your app path as well. 
  - The screenshot must be cropped into 640 x 640 pixels and in .png format.
  - the file ```index.html``` is the landing page of your app.
    Use ```/js/angular.min.js``` from the loklak root path for your application.
    The app should make use of the json libraries in ```html/js```.
    If applicable, make use of the bootstrap style from ```html/css```.
  - the file ```app.json``` must be in json-ld format (see http://json-ld.org/)
    and must contain the ```SoftwareApplication``` object from schema.org:
    https://schema.org/SoftwareApplication -- just copy-paste an existing ```app.json``` from another app to start you own file

3. Check quality of your app
  - do a json-ld validation: use https://developers.google.com/structured-data/testing-tool/ to check your ```app.json```
  - call http://localhost:9000/api/apps.json to see if your ```app.json``` is included in the app list
  - check if all declarations in your ```app.json``` relate to your own app
    (if you copy-pasted another ```app.json```, you may have forgotten to change some fields)
  - check the style and behaviour of your app: don't deliver half-done code.
  - open your ```index.html``` in different browser to check that your code is not browser-specific
  - add a backlink in your app to ```/apps/``` to make it possible that users can browse from your app to all other apps

4. Publish your app
  - send a pull request to https://github.com/loklak/loklak_server
  - all your files must be contained into one commit

## How users will discover your app
The loklak front-end will compute an aggregation of all those app.json descriptions and
provide this in ```/api/apps.json``` as a list of the single app.json files.
A front-end (another app) will provide an overview of the given apps in visual form.
This will be linked in the loklak front-end.
