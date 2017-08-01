# chat.susi.ai

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/db948e1eb4b2457386ba80388e8390cf)](https://www.codacy.com/app/rishiraj824/chat.susi.ai?utm_source=github.com&utm_medium=referral&utm_content=fossasia/chat.susi.ai&utm_campaign=badger)
[![Build Status](https://travis-ci.org/fossasia/chat.susi.ai.svg?branch=master)](https://travis-ci.org/fossasia/chat.susi.ai)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/fossasia/susi_webchat?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Twitter Follow](https://img.shields.io/twitter/follow/asksusi.svg?style=social&label=Follow&maxAge=2592000?style=flat-square)](https://twitter.com/asksusi)

Susi is an artificial intelligence combining pattern matching, internet data, data flow principles and inference engine principles. It will have some reflection abilities and it will be able to remember the users input to produce deductions and a personalized feed-back. Its purpose is to explore the abilities of an artificial companion and to answer the remaining unanswered questions. The SUSI.AI web chat is a front-end that is developed for web access of SUSI.

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/group/susiai/

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_webchat

## Technology Stack

### Components
* HTML - Structure of the web page generated.
* CSS - Styling options and details ofthe web page.
* Javascript(JSON) - Used to store information for deploying the application such as dependencies.
* ReactJS - Structure for deployment of the web page.

## Requirements
* node --version >= 6
* npm --version >= 3

## How to deploy?

### Running on localhost:
* **Step 1:** Fork chat.susi.ai repository and clone it to your desktop
* **Step 2:** Then cd into that cloned folder
* **Step 3:** Install all the dependencies by running :```$ npm install```
* **Step 4:** Run on http://localhost:3000 by running :```$ npm run start```
* **Step 5:** Build locally by running : ```$ npm run build ```
* **Step 6:** To deploy at a url use : ```$ npm run deploy ```

### How to connect to Susi Hardware?
* **Step 1:** Configure your Susi Hardware Device using instructions on https://github.com/fossasia/susi_hardware
* **Step 2:** Go to settings > Connect to Susi Hardware
* **Step 3:** Add the default WebSocket URL for your Susi Hardwre Device. If you are using webchat on the same device as Susi Hardware, it will be ws://127.0.0.1:9001 . Default port is 9001, unless configured otherwise.
* **Step 4:** On successful connection, you will get a confirmation alert. After that, all your queries to your Susi Hardware Device and their results will show up on Susi Webchat.

### Colours and Font Sizes 

## Component Colours of Light theme

* Background Colour of the Application - ![#fffff](https://placehold.it/15/ffffff/000000?text=+) `#ffffff`
* Background Colour Message History -![#f5f4f6](https://placehold.it/15/f5f4f6/000000?text=+) `#f5f4f6` #f5f4f6
* Chat bubbles Colour- 
    * Chat bubbles of SUSI- ![#fffff](https://placehold.it/15/ffffff/000000?text=+) `#ffffff`
    * Chat bubbles of User-  ![#e0e0e0](https://placehold.it/15/e0e0e0/000000?text=+) `#e0e0e0`
* Top Bar Colour-  ![#4285f4](https://placehold.it/15/0084ff/000000?text=+) `#4285f4`
* Buttons Colour- ![#4285f4](https://placehold.it/15/0084ff/000000?text=+) `#4285f4`
* Colour of search result- ![#ff5e00](https://placehold.it/15/ff5e00/000000?text=+) `#ff5e00`

* Toggle Colour- 
    * thumbOnColor- ![#5ab1fc](https://placehold.it/15/5ab1fc/000000?text=+) `#5ab1fc`
    * trackOnColor- ![#4285f4](https://placehold.it/15/0084ff/000000?text=+) `#4285f4`

* User Feedback Colour-
    * Thumbs Up Colour-
         **Voted**- ![#1685e5](https://placehold.it/15/1685e5/000000?text=+) `#1685e5`
         **Unvoted**- ![#90a4ae](https://placehold.it/15/90a4ae/000000?text=+) `#90a4ae` 

## Fonts

* Font Type of Chat Message-  "Product Sans", sans-serif
* Font Type of Message Composer-  "Product Sans", sans-serif
* Chat Message font size :14px
* Chat Composer font size : 16px
* Font Colour of Chat Message- ![#001d38](https://placehold.it/15/001d38/000000?text=+) `#001d38`
* Font Colour of Message Composer- ![#001d38](https://placehold.it/15/001d38/000000?text=+) `#001d38`