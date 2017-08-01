# Telegram Installation

Consuming the Susi API with Telegram is fairly straightforward in telegram. The use of `botfather` in telegram ensures that the bots can be created with ease. So the first step is to login into telegram with your user account and search and talk to `BotFather`. Bot father would ask a few questions and then provide the required token. You need to save this token and the bot powered by susi is now available.

![Bot Father 1](/docs/images/botfather1.png)

![Bot Father 2](/docs/images/botfather2.png)

Once this is set, you need to update the token using heroku as follows

`heroku config:set TELEGRAM_ACCESS_TOKEN=AVERYLONGTOKEN:GOESOVERHEREFORYOUTORUNSUSIONTELEGRAM`
