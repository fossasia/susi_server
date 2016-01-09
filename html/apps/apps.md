# loklak Applications

loklak_server is the framework for messages, queries, suggestion, geocoding, accounts and more.
These services do not have a graphical front-end. Supporters may develop and submit front-ends
for loklak and send pull requests to add their front-ends to the loklak main release.
These front-ends are called 'apps' and must be placed here.

The rules to add an app are:
- the pull request must contain files only in one single sub-path of the apps directory
- the sub-path will be the application path for the app
- the sub-path must contain at least two files
  * a file named index.html for the front-most landing page of the app
  * a file named app.json in json-ld format which describes the app
  
The app should make use of the json libraries in html/js but if it requires additional libraries
they must be either linked directly or contained in the app path.
The required app.json must be a schema.org object of type SoftwareApplication.
See https://schema.org/SoftwareApplication for a complete documentation.
The author must test if the app.json is valid using the tool provided in 
https://developers.google.com/structured-data/testing-tool/

The loklak front-end will compute an aggregation of all those app.json descriptions and
provide this in /api/apps.json as a list of the single app.json files.
A front-end (another app) will provide an overview of the given apps in visual form

