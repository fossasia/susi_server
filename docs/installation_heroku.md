#How to run the Loklak server on Heroku using Toolbelt

1. Create a Heroku account https://www.heroku.com/
2. Download the Heroku toolbelt https://toolbelt.heroku.com/
3. Login with heroku: `heroku login`
4. Clone the Loklak server (if not already) : `git clone https://github.com/loklak/loklak_server.git`
5. Move into the cloned repository: `cd loklak_server`
6. Create a heroku app: `heroku create`
7. Set the buildpack: `heroku buildpacks:set https://github.com/loklak/heroku_buildpack_ant_loklak.git`
8. Push your app to heroku: `git push heroku master`
9. Confirm the loklak server is running: `heroku logs --tail`
10. Sometimes the server may take a while to start. The logs would show `State changed from starting to up` when the server is ready.
11. Open the URL of your server in your browser: `heroku open`.
