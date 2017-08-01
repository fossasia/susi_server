# iOS App for Susi

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4faa165463a44fffbd23f319d78a26ea)](https://www.codacy.com/app/mb/susi_iOS?utm_source=github.com&utm_medium=referral&utm_content=fossasia/susi_iOS&utm_campaign=badger)
[![Join the chat at https://gitter.im/fossasia/susi_iOS](https://badges.gitter.im/fossasia/susi_iOS.svg)](https://gitter.im/fossasia/susi_iOS?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CircleCI](https://circleci.com/gh/fossasia/susi_iOS.svg?style=svg)](https://circleci.com/gh/fossasia/susi_iOS)
[![Preview the app](https://img.shields.io/badge/Preview-Appetize.io-orange.svg)](https://appetize.io/app/bngee02t60ambqz5ed3kjgfgkm)

The main feature of the app is to provide a conversational interface to provide intelligent answers using the loklak/AskSusi infrastructure. The app also offers login functionalities to connect to other services and stored personal data. Additionally the application uses data provided by the user's phone to improve Susi answers. Geolocation information for example helps to offer better answers related to questions about "things nearby".

## Roadmap

Make the app functionality and UI/UX similar to the android app for Susi.

## Development Setup

Before you begin, you should already have the Xcode downloaded and set up correctly. You can find a guide on how to do this here: [Setting up Xcode](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppStoreDistributionTutorial/Setup/Setup.html)

##### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Steps to install Cocoapods (one time installation)

- Run `sudo gem install cocoapods` to install the latest version of cocoapods

-  Next, run `pod setup` for setting up cocoapods master repo. You may include `--verbose` for more descriptive logs.
**NOTE:** This might take a while to setup depending on your network speed.

## Setting up the iOS Project

1. Download the _susi_iOS_ project source. You can do this either by forking and cloning the repository (recommended if you plan on pushing changes) or by downloading it as a ZIP file and extracting it.

2. Navigate to the unzipped folder and run `pod install`.

3. Open `Susi.xcworkspace` from the folder.

4. Build the project (⌘+B) and check for any errors.

5. Run the app (⌘+R).and test it.

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/opntec-dev

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_iOS

## Development

A native iOS app.

## Screenshots

  <p align="center">
    <img src="docs/img/Screen1.png" height = "480" width="270"> 
    <img src="docs/img/Screen2.png" height = "480" width="270"> 
    <img src="docs/img/Screen3.png" height = "480" width="270">
  </p>

  <p align="center">
    <img src="docs/img/Screen4.png" height = "480" width="270"> 
    <img src="docs/img/Screen5.png" height = "480" width="270"> 
    <img src="docs/img/Screen6.png" height = "480" width="270">
  </p>

## Branch Policy

Note: For the initialization period all commits go directly to the master branch. In the next stages we follow the branch policy as below:

We have the following branches
* **ipa**
All the automatic builds generates, i.e., the ipas go into this branch
* **master**
This contains shipped code. After significant features/bugfixes are accumulated on development, we make a version update, and make a release.
* **development**
All development goes on in this branch. If you're making a contribution,
you are supposed to make a pull request to _development_.


## Code practices

Please help us follow the best practice to make it easy for the reviewer as well as the contributor. We want to focus on the code quality more than on managing pull request ethics. 

* Single commit per pull request
* For writing commit messages please read the [COMMITSTYLE](docs/commitStyle.md) carefully. Kindly adhere to the guidelines.
* Follow uniform design practices. The design language must be consistent throughout the app.
* The pull request will not get merged until and unless the commits are squashed. In case there are multiple commits on the PR, the commit author needs to squash them and not the maintainers cherrypicking and merging squashes.
* If the PR is related to any front end change, please attach relevant screenshots in the pull request description.

## License

This project is currently licensed under the Apache License Version 2.0. A copy of [LICENSE.md](https://github.com/fossasia/susi_iOS/blob/master/LICENSE) should be present along with the source code. To obtain the software under a different license, please contact FOSSASIA.


