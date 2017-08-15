# Slack Installation for Susi

**NOTE:** You can add the Susi Slack Bot to your team by clicking on the Add to Slack button. Follow these instructions if you wish to make a bot programatically.


Making your own Slack bot for Susi is fairly simple, given the convenient integration of bot users into the Slack platform. A more detailed documentation of how bot users work is given [here](https://api.slack.com/bot-users).

There are three ways of setting up a Slack Bot programatically:

1. Directly use the Slack API (or a wrapper around it), and post to your bot.

2. Use an Incoming Webhook, get the Incoming Webhook URL, and POST a payload (a JSON which has the message your bot needs to give) to that URL.

3. Use an Outgoing Webhook, set up the Trigger words and the URL endpoint where your bot should listen from.

## Basic Setup

### If you wish to set up the bot only for your team

1. Log in to your Slack team.

2. Go to the Custom Bots page [here](https://my.slack.com/services/new/bot), and add a username for your bot. Click on ```Add bot Integration```.

3. Once done, you will be led to the Settings panel of your bot, where you will have the normal bot settings (username, icon etc) and a Slack API token. Use the API token for making authorised API calls to the Slack API. Most bots use the [Real-time Messaging API](https://api.slack.com/rtm). 

4. You can now use the API token, and use the Slack API, and code up your bot. 

### If you wish to make a bot and distribute it later

1. Log in to your Slack Team.

2. Go to the Apps page [here](https://api.slack.com/slack-apps) and click on ```Create an App```.

3. Fill out the form. You should especially fill out the ```redirect_uri``` (it's not compulsory but you'll have to fill it sometime) since it is needed for OAuth (when other users use your bot). Once form filled, click on ```Add App```.

4. Write your app code. You should make a method which handles OAuth, as described in Step 5. 

5. Once done, get the "Add to Slack" button from [here](https://api.slack.com/docs/slack-button). The redirect_uri should be able to handle the OAuth (the parameters are ```client_id```, ```client_secret``` and a code that the slack button gives). 

	Once the user is authorised, you get the Slack ```access-token```, which you should add into your app. So make your app such that it takes in the access-token after the user is authorised (you can easily do it by setting a route in your app, add that URL as the ```redirect_uri```, and within that route, write a method so that it parses the JSON returned by the OAuth request and takes in the access token). More info on handling the OAuth can be found [here](https://api.slack.com/docs/oauth). 

6. You're all set. Publicise your Add to Slack button and make users add your bot to their teams. 