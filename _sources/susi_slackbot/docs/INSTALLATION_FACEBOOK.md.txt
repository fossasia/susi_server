# Facebook Messenger Installation

It's easy for you to create your own facebook messenger bot and integrate susi's API into it. You can read the  [documentation](https://developers.facebook.com/docs/messenger-platform/quickstart) the Messenger team prepared.

Messenger bots uses a web server to process messages it receives or to figure out what messages to send. You also need to have the bot be authenticated to speak with the web server and the bot approved by Facebook to speak with the public.

## Setup your own Messenger Bot

1. Create or configure a Facebook App or Page here https://developers.facebook.com/apps/
2. In the app go to Messenger tab then click Setup Webhook. Here you will put in the URL of your Heroku server and a token. Make sure to check all the subscription fields. 
![FB Settings](/docs/images/fb_settings.png)
3. Get a Page Access Token and save this somewhere. 
4. Go back to Terminal and type in this command to trigger the Facebbook app to send messages. Remember to use the token you requested earlier.

```bash
curl -X POST "https://graph.facebook.com/v2.6/me/subscribed_apps?access_token=<PAGE_ACCESS_TOKEN>"
```
