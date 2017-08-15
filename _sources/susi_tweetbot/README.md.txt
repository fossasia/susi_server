# susi_tweetbot
[![Build Status](https://travis-ci.org/fossasia/susi_tweetbot.svg?branch=master)](https://travis-ci.org/fossasia/susi_tweetbot)

# How to chat with Susi AI bot on Twitter
Visit your twitter account and tweet to @SusiAI1 with your query and enjoy a tweet back from the Susi AI bot account!

Also, you can enjoy personal chatting with Susi. Follow the SUSI AI account on twitter [here](https://twitter.com/SusiAI1) and have a personal chat with it.
![twitter chat](/docs/images/twitterChat.PNG)

# Getting Started : Twitter Susi AI Bot Installation 
We will make a Susi messenger bot account on Twitter. This account will tweet back when it's name is mentioned in a tweet. Also, 1-on-1 auto chat will be inculcated to our account.

Make a new account, which you want to use as the bot account. You can make one from [here](https://www.twitter.com).

## Prerequisites
To create your account on -:
1. Twitter
2. Github
3. Heroku

## Setup your own Messenger Bot
1. Fork this repository.

2. Make a new app [here](https://apps.twitter.com/app/new), to know the access token and other properties for our application. These properties will help us communicate with Twitter.
 ![twitter app](docs/images/twitterAppNew.PNG)
 
 Click "modify the app permissions" link, as shown here:
 ![twitter keys](/docs/images/TwitterKeys1.PNG)
 
 Select the Read, Write and Access direct messages option:
 ![twitter permissions](/docs/images/twitterAccessPermissions.PNG)
  
 Don't forget to click the update settings button at the bottom.
 
 Click the Generate My Access Token and Token Secret button.
 
3. Create a new heroku app [here](https://dashboard.heroku.com/new?org=personal-apps).

 This app will accept the requests from Twitter and Susi api.
 ![create heroku app](/docs/images/createHerokuApp.png)

4. Create a config variable by switching to settings page of your app.
   
   The name of your first config variable should be HEROKU_URL and its value is the url address of the heroku app created by you. 
   ![config variables](docs/images/configVariables.PNG)
   
   The other config variables that need to be created will be these:
   ![twitter config variables](/docs/images/twitterConfigVariables.PNG)
   
   The corresponding names of these variables in the same order are:
   1. Access token
   2. Access token secret
   3. Consumer key
   4. Consumer secret
   
   We need to visit our app from [here](https://apps.twitter.com), the keys and access tokens tab will help us with the values of these variables.

5. Connect the heroku app to the forked repository.
 
 Connect the app to Github by selecting the name of this forked repository.
 ![heroku github connect](/docs/images/herokuGithubConnect.png)

6. Deploy on development branch. If you intend to contribute, it is recommended to Enable Automatic Deploys.

 Branch Deployment.
 ![branch selection](/docs/images/branchSelection.png)

 Successful Deployment.
 ![heroku deployment](/docs/images/herokuDeployment.png)

7. Visit your own personal account and tweet to this new bot account with your query and enjoy a tweet back from the bot account!

8. Also, you can enjoy personal chatting with Susi.
 ![twitter chat](/docs/images/twitterChat.PNG)
 
 Feel free to play around with the already made SUSI AI account on twitter [here](https://twitter.com/SusiAI1). Follow it, to have a personal chat with it.
