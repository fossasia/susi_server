# How to run the Susi server on Heroku using Toolbelt

1. Create a [Heroku account](https://www.heroku.com/)
2. Download the [Heroku toolbelt](https://toolbelt.heroku.com/)
3. Login with heroku: `heroku login`
4. Clone the Susi server and update the submodules: 
	```
	git clone --recursive https://github.com/fossasia/susi_server.git
	```
5. Move into the cloned repository: `cd susi_server`
6. Update submodules: `git submodule update --init --recursive`
7. Create a heroku app: `heroku create [optional: app name]`
8. Push the development branch to heroku: `git push heroku development:master`
9. Confirm the susi server is running: `heroku logs --tail`
10. Sometimes the server may take a while to start. The logs would show `State changed from starting to up` when the server is ready.
11. Open the URL of your server in your browser: `heroku open`.
