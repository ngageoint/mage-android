# MAGE Android

11/05/2015

This is the MAGE client for Android devices.

![MAGE](screenshots/composite.png)

Depends on the [MAGE Android SDK](https://github.com/ngageoint/mage-android-sdk).

## About

The **M**obile **A**wareness **G**EOINT **E**nvironment, or MAGE, provides mobile situational awareness capabilities. The MAGE app on your mobile device allows you to create geotagged field reports that contain media such as photos, videos, and voice recordings and share them instantly with who you want. Using the GPS in your mobile device, MAGE can also track users locations in real time. Your locations can be automatically shared with the other members of your team.

The app remains functional if your mobile device loses its network connection, and will upload its local content when a connection is re-established. When disconnected from the network, MAGE will use local data layers to continue to provide relevant GEOINT. Data layers, including map tiles and vector data, can stored on your mobile device and are available at all times.

MAGE is very customizable and can be tailored for you situation.

MAGE Android was developed at the National Geospatial-Intelligence Agency (NGA) in collaboration with BIT Systems. The government has "unlimited rights" and is releasing this software to increase the impact of government investments by providing developers with the opportunity to take things in new directions. The software use, modification, and distribution rights are stipulated within the Apache license.


## How to Build

These instructions are for *nix operating systems.  Do not use Windows systems to build MAGE.

### Setup & Configuration

Android requires that you sign your applications.  You will need to create an idenity to sign your MAGE Android build.  To do this, create a new keystore file using the keytool utility:
```bash
keytool -genkey -v -keystore ~/debug.keystore -alias magedebugkey -keyalg RSA -validity 14000
```

keytool will prompt for a password of your choice and other information that identifies you.  When finished, *~/debug.keystore* should now exist on your system.  Verify the information you entered looks correct:
```bash
keytool -list -keystore ~/debug.keystore
```

MAGE Android uses Google Maps.  Whether you build a dubug, or release version of the application, you will need to obtain a Google Maps API key to use Android Google's Maps.

First, go to [Google's API Library](https://console.developers.google.com/) with a valid Google account, and enable the *Google Maps Android API* in the *APIs & Auth* section.  Next, in the *credentials* section, *Add Credentials* -> *API key* -> *Android key*.  Enter a key name, or accept the default.  Enter *mil.nga.giat.mage* for the package name, and the SHA-1 certificate fingerprint from the keystore you made.  You can get the SHA-1 hash from the debug.keystore you made using the command line:
```bash
keytool -list -keystore ~/debug.keystore
```

Finally, click *create* in Google's developer console.  Once created, you should be prompted with a hash that represents the Android Google Maps API key.

To link the mage applicaiton with this api key, you will need to edit the *DEBUG_MAPS_API_KEY* value in [gradle.properties file](gradle.properties).  Congratulations!  You are all done configuring your very own debug build of MAGE Android.

### Build

The MAGE Android application (apk) is built using [gradle](http://gradle.org/).  These instructions build a debug version of the application that is **not for release**.

Before you build the MAGE Android applicaiton, make sure to download the and build the MAGE [sdk](https://github.com/ngageoint/mage-android-sdk) first.

This command will create the Android package that you will install on the phone:

```bash
./gradlew clean
./gradlew assembleLocalDebug
file ./mage/build/outputs/apk/mage-local-debug.apk
```

### Install
```bash
./gradlew installLocalDebug
```

### Test
```bash
./gradlew connectedAndroidTestLocalDebug
```

## Pull Requests

If you'd like to contribute to this project, please make a pull request. We'll review the pull request and discuss the changes. All pull request contributions to this project will be released under the Apache license.

Software source code previously released under an open source license and then modified by NGA staff is considered a "joint work" (see 17 USC § 101); it is partially copyrighted, partially public domain, and as a whole is protected by the copyrights of the non-government authors and must be released according to the terms of the original open source license.

## License

Copyright 2015 BIT Systems

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
