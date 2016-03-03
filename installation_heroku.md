#How to run the Loklak server on Heroku

1. Create a Heroku account https://www.heroku.com/
2. Download the Heroku toolbelt https://toolbelt.heroku.com/
3. Login with heroku: `heroku login`
4. Clone the Loklak server (if not already) : `git clone https://github.com/loklak/loklak_server.git`
5. Create a heroku app: `heroku create`
6. Set the buildpack: `heroku buildpacks:set https://github.com/aneeshd16/heroku-buildpack-ant-loklak.git`
7. Push your app to heroku: `git push heroku master`
8. Confirm the loklak server is running: `heroku logs --tail`
9. Sometimes the server may take a while to start. The logs would show `State changed from starting to up` when the server is ready.
9. Open the URL of your server in your browser: `heroku open`.
