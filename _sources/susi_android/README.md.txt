# Android App for Susi

[![CircleCI](https://circleci.com/gh/fossasia/susi_android.svg?style=svg&branch=development)](https://circleci.com/gh/fossasia/susi_android)
[![Gitter](https://badges.gitter.im/fossasia/susi_android.svg)](https://gitter.im/fossasia/susi_android?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6ec0032213274fa0a07574919928c6a6)](https://www.codacy.com/app/harshithdwivedi/susi_android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=fossasia/susi_android&amp;utm_campaign=Badge_Grade)
[![Preview the app](https://img.shields.io/badge/Preview-Appetize.io-orange.svg)](https://appetize.io/app/mbpprq4xj92c119j7nxdhttjm0)
[![Mailing List](https://img.shields.io/badge/Mailing%20List-FOSSASIA-blue.svg)](mailto:fossasia@googlegroups.com)

The main feature of the app is to provide a conversational interface to provide intelligent answers using the loklak/AskSusi infrastructure. The app also offers login functionalities to connect to other services and stored personal data. Additionally the application uses data provided by the user's phone to improve Susi answers. Geolocation information for example helps to offer better answers related to questions about "things nearby".

## Roadmap

Planned features & enhancements are:
- Hotword Detection training
- Full Screen Voice interaction
- Feedback for skills.
- Displaying SUSI skills 

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/susiai

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_android

## Screenshots

<table>
  <tr>
    <td><img src="docs/_static/login.png" height = "480" width="270"></td>
    <td><img src="docs/_static/signup.png" height = "480" width="270"></td>
    <td><img src="docs/_static/message.png" height = "480" width="270"></td>
  </tr>
  <tr>
    <td><img src="docs/_static/message_map.png" height = "480" width="270"></td>
    <td><img src="docs/_static/message_select.png" height = "480" width="270"></td>
    <td><img src="docs/_static/settings.png" height = "480" width="270"></td>
  </tr>
</table>

## Development

A native Android app using both Java and Kotlin for writing code. The answers for user queries comes from [SUSI Server](https://github.com/fossasia/susi_server) which further uses skills defined in [SUSI Skill Data](https://github.com/fossasia/susi_skill_data).

### Android App Development Set up

Please find info about the set up of the Android app in your development environment [here](docs/Android_App_Setup.md).

### Libraries used and their documentation

- Realm [Docs](https://realm.io/docs/java/latest/)
- Retrofit [Docs](http://square.github.io/retrofit/2.x/retrofit/)
- ButterKnife [Docs](http://jakewharton.github.io/butterknife/javadoc/)
- Espresso [Docs](https://google.github.io/android-testing-support-library/docs/espresso/)
- Tajchert Waiting Dots [Docs](https://github.com/tajchert/WaitingDots)
- Picasso [Docs](http://square.github.io/picasso/)
- LeakCanary [Docs](https://github.com/square/leakcanary)
- LeonardoCardoso/Android-Link-Preview [Docs](https://github.com/LeonardoCardoso/Android-Link-Preview)

### Project Conventions

There is certain conventions we follow in the project, we recommend that you become familiar with these so that the development process is uniform for everyone:

#### MVP

The project follows Model-View-Presenter design pattern and requires schematic interfaces for each component to be written first as contracts and then implemented.   
All the interactions are done using interfaces only. This means any model, view or presenter will only be referenced by its interface. We do so it is easy to mock and test them and there is no discrepancy in the callable methods of the concrete class and the interface.  
We realize that MVP is opinionated and there is no strict boundary between the responsibility of each component, but we recommend following this style:
- `View` is passive and dumb, there is no logic to be exercised in View, only the ability to show data provided by the presenter through contract is present in the View. This makes it easy to unit test and remove the dependence on Android APIs, thus making the need of instrumentation tests scarce
- `Presenter` is responsible for most of the business logic, manipulation of data and organising it for the view to present. All logic for the app is present here and it is devoid of ANY Android related code, making it 100% unit testable. We have created wrapper around common Android APIs in form of models so that they can be mocked and presenter stays clean. The responsibility of presenter includes the fetching of data from external source, observing changes and providing the view with the latest data. It also needs to handle all View interactions that require any logic, such as UI triggers causing complex interactions. Notable exception for this is launching of an Activity on click of a button. There is no logic required in the action and is completely dependent on Android APIs. Lastly, presenter should always clean up after the view is detached to prevent memory leaks
- `Model` has the responsibility to hold the data, load it intelligently from appropriate source, be it disk or network, monitor the changes and notify presenter about those, be self sufficient; meaning to update data accordingly as needed without any external trigger (saving the data in disk after updating from network and providing the saved data from next time on), but be configurable (presenter may be able to ask for fresh data from network). The presenter should not worry about the data loading and syncing conditions (like network connectivity, failed update, scheduling jobs, etc) as it is the job of model itself.

#### Use of Kotlin 

Around 50% of the App is written in [Kotlin](https://kotlinlang.org/). Kotlin is a very similar language to Java but with much more advantages then Java. It is easy to adapt and learn. So, you need not worry if you don't have prior experience with it. Follow [these](https://kotlinlang.org/docs/reference/) docs for syntax reference. The latest Android Canary Version has in built support for Kotlin but if you don't have the Canary version, you can add Kotlin Plugin in your Android Studio. Follow [this](https://android.jlelse.eu/setup-kotlin-for-android-studio-1bffdf1362e8) link to see how to do that.

#### Project Structure

Generally, projects are created using package by layer approach where packages are names by layers like `ui`, `activity`, `fragment`, etc but it quickly becomes unscalable in large projects where large number of unrelated classes are crammed in one layer and it becomes difficult to navigate through them.  
Instead, we follow package by feature, which at the cost of flatness of our project, provides us packages of isolated functioning related classes which are likely to be a complete self sufficient component of the application. Each package all related classes of view, presenter, their implementations like Activities anf Fragments.  
A notable exception to this is the `helper` module and data classes like Models and Repositories as they are used in a cross component way.  
***Note:** The interface contract for Presenter and View is present in `contract` package in each module`*

#### Separation of concerns

Lastly, each class should only perform one task, do it well, and be unit tested for it. For example, if a presenter is doing more than it should, i.e., parsing dates or implementing search logic, better move it in its own class. There can be exceptions for this practice, but if the functionality can be generalised and reused, it should most definitely be transferred in its own class and unit tested.

## Contributions Best Practices

### Branch Policy

We have the following branches

 * **development** All development goes on in this branch. If you're making a contribution, you are supposed to make a pull request to _development_. PRs to gh-pages must pass a build check and a unit-test check on Travis.
 * **master** This contains shipped code. After significant features/bugfixes are accumulated on development, we make a version update, and make a release.
 * **apk** This branch contains two apk's, that are automatically generated on merged pull request a) debug apk and b) release apk.

### Code practices

Please help us follow the best practice to make it easy for the reviewer as well as the contributor. We want to focus on the code quality more than on managing pull request ethics. 

 * Single commit per pull request
 * For writing commit messages please read the [COMMITSTYLE](docs/commitStyle.md) carefully. Kindly adhere to the guidelines.
 * Follow uniform design practices. The design language must be consistent throughout the app.
 * The pull request will not get merged until and unless the commits are squashed. In case there are multiple commits on the PR, the commit author needs to squash them and not the maintainers cherrypicking and merging squashes.
 * If the PR is related to any front end change, please attach relevant screenshots in the pull request description.

### Join the development

* Before you join development, please set up the project on your local machine, run it and go through the application completely. Press on any button you can find and see where it leads to. Explore. (Don't worry ... Nothing will happen to the app or to you due to the exploring :wink: Only thing that will happen is, you'll be more familiar with what is where and might even get some cool ideas on how to improve various aspects of the app.)
* If you would like to work on an issue, drop in a comment at the issue. If it is already assigned to someone, but there is no sign of any work being done, please free to drop in a comment so that the issue can be assigned to you if the previous assignee has dropped it entirely.

## For Developers: Adding Fabric API KEY
1. Go to AndroidFest.xml
Replace the fabric_api_key with the Real Fabric API Key
Add: <meta-data android:name="io.fabric.ApiKey" android:value="fabric_api_key" /> 

2. Open the app/fabric.properties:
Replace the fabric_api_key with your actual Fabric API Secret.

3. Open MainApplication.java, 
	a) After adding the API KEYS and API Secret
	Uncomment the line: Fabric.with(this, new Crashlytics()) 

	b) Add imports :
		import com.crashlytics.android.Crashlytics;
		import io.fabric.sdk.android.Fabric;

4. Uncomment the line in the app/gradle
	Line: apply plugin: 'io.fabric'
    
## For Testers: Testing the App
If you are a tester and want to test the app, you have two ways to do that:
1. **Installing APK on your device:** You can get debug [APK](https://github.com/fossasia/susi_android/blob/apk/susi-debug.apk) as well as Release [APK](https://github.com/fossasia/susi_android/blob/apk/susi-release.apk) in apk branch of the repository. After each PR merge, both the APKs are automatically updated. So, just download the APK you want and install it on your device. The APKs will be always be latest one. 
2. **Testing on [appetize.io](https://appetize.io/app/mbpprq4xj92c119j7nxdhttjm0):** If you don't want to download the APKs, you can simply go on [this](https://appetize.io/app/mbpprq4xj92c119j7nxdhttjm0) link and use the App on an online simulator. You will always find latest version of App on that link because it is updated after each PR merge.

## License

This project is currently licensed under the Apache License Version 2.0. A copy of [LICENSE.md](https://github.com/fossasia/susi_android/blob/master/LICENSE) should be present along with the source code. To obtain the software under a different license, please contact FOSSASIA.
