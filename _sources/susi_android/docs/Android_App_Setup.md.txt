# Android App Setup

## Development Setup

Before you begin, you should already have the Android Studio SDK downloaded and set up correctly. You can find a guide on how to do this here: [Setting up Android Studio](http://developer.android.com/sdk/installing/index.html?pkg=studio)

## Setting up the Android Project

1. Download the _susi_android_ project source. You can do this either by forking and cloning the repository (recommended if you plan on pushing changes) or by downloading it as a ZIP file and extracting it.

2. Open Android Studio, you will see a **Welcome to Android** window. Under Quick Start, select _Import Project (Eclipse ADT, Gradle, etc.)_

3. Navigate to the directory where you saved the susi_android project, select the root folder of the project (the folder named "susi_android"), and hit OK. Android Studio should now begin building the project with Gradle.

4. Once this process is complete and Android Studio opens, check the Console for any build errors.

  - _Note:_ If you receive a Gradle sync error titled, "failed to find ...", you should click on the link below the error message (if avaliable) that says _Install missing platform(s) and sync project_ and allow Android studio to fetch you what is missing.

5. Once all build errors have been resolved, you should be all set to build the app and test it.

6. To Build the app, go to _Build>Make Project_ (or alternatively press the Make Project icon in the toolbar).

7. If the app was built successfully, you can test it by running it on either a real device or an emulated one by going to _Run>Run 'app'_ or pressing the Run icon in the toolbar.

## Configuring the app

**Configuring App Theme / Localizations**

- The styles.xml files have been configured to allow easy customization of app themes.

- You can configure themes by changing various components found in the styles.xml files, found at:

  - _/app/src/main/res/values/styles.xml_
  - _/app/src/main/res/values-v21/styles.xml_

- Using _Theme Editor_:

  - You can also configure the theme of the app using Android Studio's _Theme Editor_.
  - Go to _Tools>Android>Theme Editor_ to open the Theme Editor.
  - From there you can configure the colors and styles of in-app elements using a neat UI.

- _Translations Editor_:

  - You can configure the string localizations / translations using Android Studio's _Translations Editor_.
  - Find /app/src/main/res/values/strings.xml
  - Right click on the file, and select _Open Translations Editor_.

- Editing Manually:

  - You can find the configuration files for the app for manual editing here:
  - _/app/src/main/res/values/_
  - _/app/src/main/res/values-v21/_
  - _/app/src/main/res/values-w820dp/_
