# How to run the Susi server on Heroku using Toolbelt

1. Create a [Heroku account](https://www.heroku.com/)
2. Download the [Heroku toolbelt](https://toolbelt.heroku.com/)
3. Login with heroku: `heroku login`
4. Clone the Susi server and update the submodules: 
	```
	git clone --recursive https://github.com/fossasia/susi_server.git
	git submodule update --init --recursive
	```
5. Move into the cloned repository: `cd susi_server`
6. Create a heroku app: `heroku create [optional: app name]`
7. Push the development branch to heroku: `git push heroku development:master`
8. Confirm the susi server is running: `heroku logs --tail`
9. Sometimes the server may take a while to start. The logs would show `State changed from starting to up` when the server is ready.
10. Open the URL of your server in your browser: `heroku open`.
