# Change Log
All notable changes to this project will be documented in this file.
Adheres to [Semantic Versioning](http://semver.org/).

---
## 6.3.1 (TBD)

##### Features

##### Bug Fixes

## [6.3.0](https://github.com/ngageoint/mage-android/releases/tag/6.3.0)

##### Features
* Google authentication support is back. This includes updates for all third party authentication strategies to align with Google guidelines.
* Support for Android 11.  Includes updates to handle new location permissions.
* Upgrade Gradle and Kotlin versions.

##### Bug Fixes

## [6.2.10](https://github.com/ngageoint/mage-android/releases/tag/6.2.10)

##### Features
* Show observation accuracy if available.  Shown on map as accuracy circle and in observation view as text.
* Use algorithm to determine "best" device location based on provider, accuracy and time to determine
  new observation location.

##### Bug Fixes

## [6.2.9](https://github.com/ngageoint/mage-android/releases/tag/6.2.9)

##### Features
* Observation feed show/hide primary/secondary form feed fields.

##### Bug Fixes
* Work around patch for Google Maps bug 154855417.  This will remove data pushed by Google Maps causing application to crash.
* Fix for LDAP authentication.

## [6.2.8](https://github.com/ngageoint/mage-android/releases/tag/6.2.8)

##### Features
* Update support library to androidx
* Improved GeoPackage functionality including styles, scaling, and zoom
* geopackage-android-map version 3.4.0
* android-maps-utils version 0.6.2
* Moved geopackages and static features (e.g. XML) into a new preference called offline layers.
  These offline layers only exist on a remote server until downloaded onto the client.
* Online layers preference added.  This gives users access to layers that exist on remote XYZ, TMS,
  and WMS servers without needing to be downloaded.  Only supports HTTPS.
* Moved Observations and People preferences to new preference category called MAGE.

##### Bug Fixes

## [6.2.7](https://github.com/ngageoint/mage-android/releases/tag/6.2.7)

##### Features
* Traffic map layer

##### Bug Fixes

## [6.2.6](https://github.com/ngageoint/mage-android/releases/tag/6.2.6)

##### Features

##### Bug Fixes
* User avatars now load in feed and profile page
* Fix crash when trying to add audio attachments on some Samsung devices
* Pull observations after switching events

## [6.2.5](https://github.com/ngageoint/mage-android/releases/tag/6.2.5)

##### Features

##### Bug Fixes
* Fixed a bug where offline users switching events were unable to create observations.
* Fixed a bug causing events with no forms to crash on observation create.

## [6.2.4](https://github.com/ngageoint/mage-android/releases/tag/6.2.4) (06-06-2019)

##### Features

##### Bug Fixes
* Handle empty/null geometry default value
* Properly handle empty recent event ids list
* Fix bug where geometry form field default wasn't showing in form view after setting default

## [6.2.3](https://github.com/ngageoint/mage-android/releases/tag/6.2.3) (05-30-2019)

##### Features

##### Bug Fixes
* Glide attachment image url loader should not set Authorization header as its already set by OkHttp3 interceptor

## [6.2.2](https://github.com/ngageoint/mage-android/releases/tag/6.2.2) (01-22-2019)

##### Features
* Update to Android SDK 28.
* Location service nows runs as a proper foreground service.
* Observations and attachments are being pulled and pushed via WorkManager.
* Updated okhttp, retrofit and glide dependencies.
* Using Dagger for dependency injection.
* GeoPackage features that contain links are clickable.
* Added 5 and 30 second user location fetch frequencies.

##### Bug Fixes
* Fix bug which didn't show previsouly loaded events after disconnected login
* Use dark background color on map infowindows, this will show light text.
* Fix bug in landing activity that could cause crash when setting options on navigation view menu.

## [6.2.1](https://github.com/ngageoint/mage-android/releases/tag/6.2.1) (10-22-2018)

##### Features
* Move to Android provided geocoder.  The provided geocoder does not need an API key, at least for now.

##### Bug Fixes

## [6.2.0](https://github.com/ngageoint/mage-android/releases/tag/6.2.0) (09-18-2018)

##### Features
* Add mgrs edit/view feature.
* Show error message from server on invalid login attempt.

##### Bug Fixes
* Don't remove token before logout request.

## [6.1.2](https://github.com/ngageoint/mage-android/releases/tag/6.1.2) (06-25-2018)

##### Features
* Beta Geopackage download feature.  Server administrators can now upload geopackages as layers
  and assign to specific events.  App users can choose to download available geopackages
  from the layer manager.

##### Bug Fixes
* Show change event in nav drawer if user has more than one event.
* Fix mgrs bug in which grid would not show on map on initial load.
* Use map type from user preferences in observation location edit activity.
* Fix brand icon color on older devices.

## [6.1.1](https://github.com/ngageoint/mage-android/releases/tag/6.1.1) (05-24-2018)

##### Features

##### Bug Fixes
* Show change event in nav drawer if user has more than one event.

## [6.1.0](https://github.com/ngageoint/mage-android/releases/tag/6.1.0) (04-16-2018)

##### Features
* Pull users, teams and icons based on event picked.  We no longer pull all users, teams and icons for all events when logging in, this will vastly
  improve performance on login.
* Added Night theme. Users can now maunally switch betweem light (current) an dark themes, or automatically switch based on Androids day/night configuration.
* Support generic oauth configuration coming from server authentication strategies.

##### Bug Fixes

## [6.0.4](https://github.com/ngageoint/mage-android/releases/tag/6.0.4) (02-19-2018)

##### Features

##### Bug Fixes
* Fix crash when trying to edit observation location
* Fix observation and location time filtering
* Fix observation and location info window click crash
* Fix map layout height in profile view

## [6.0.3](https://github.com/ngageoint/mage-android/releases/tag/6.0.3) (02-14-2018)

##### Features

##### Bug Fixes
* Don't show archived forms in form selection activity
* Prevent crash if no location when clicking zoom to location
* Clean up MapFragment memory leaks
* Prevent crash on logout due to no current event

## [6.0.2](https://github.com/ngageoint/mage-android/releases/tag/6.0.2) (01-25-2018)

##### Features

##### Bug Fixes
* Perform initial fetch before events screen.  This will ensure all data is available before we hit the landing activity

## [6.0.1](https://github.com/ngageoint/mage-android/releases/tag/6.0.1) (01-23-2018)

##### Features

##### Bug Fixes
* Fix layout for event title in event loading activity
* Pull in all events when getting events for the current user

## [6.0.0](https://github.com/ngageoint/mage-android/releases/tag/6.0.0) (01-15-2018)

##### Features
* Observation geometry support for lines and polygons
* Support for multiple forms per event
* Allow users to delete observations
* Allow users to change password from profile view
* New MGRS grid overlay.  You can also search for an MGRS string from the map.

##### Bug Fixes

## [5.3.5](https://github.com/ngageoint/mage-android/releases/tag/5.3.5) (10-10-2017)

##### Features

##### Bug Fixes
* Fix static feature map click
* Fix static feature preference loading

## [5.3.4](https://github.com/ngageoint/mage-android/releases/tag/5.3.4) (10-05-2017)

##### Features

##### Bug Fixes
* Pull new version of mage-android-sdk, fixes bug parsing multiple choice values that contained spaces.

## [5.3.3](https://github.com/ngageoint/mage-android/releases/tag/5.3.3) (09-01-2017)

##### Features

##### Bug Fixes
* Update event name in title when switching events.

## [5.3.2](https://github.com/ngageoint/mage-android/releases/tag/5.3.2) (08-22-2017)

##### Features

##### Bug Fixes
* Fixed bug causing non-required select fields to show as required

## [5.3.1](https://github.com/ngageoint/mage-android/releases/tag/5.3.1) (07-20-2017)

##### Features
* Add network config

##### Bug Fixes

## [5.3.0](https://github.com/ngageoint/mage-android/releases/tag/5.3.0) (06-15-2017)

##### Features
* Added pull to refresh on observation and people lists.
* Pulled in appcompat and support libraries to support material design.  
* Updated all views to be consistent with material design spec.
* Added new BottomNavigationView to allow users to quickly cycle through observations,
  people and the map.  
* Take users to login screen on token expiration.  Upon successful authentication the user will be taken
  back to where they left off before their token expired.
* Choose between Local time and GMT for display and editing

##### Bug Fixes
* Fixed bug with being authenticated but not having picked an event.  In this case the user will be
  returned to the event picker activity.
* Updated android.hardware.telephony to not be required, which will enable tablet installation from the play store.

## [5.2.2](https://github.com/ngageoint/mage-android/releases/tag/5.2.2) (02-13-2017)

##### Features

##### Bug Fixes
* Fixed an issue where attaching a image to an observation would cause the application to crash in some instances.

## [5.2.1](https://github.com/ngageoint/mage-android/releases/tag/5.2.1) (01-31-2017)

##### Features

##### Bug Fixes
* Bug fix preventing app crash when opening filter drawer

## [5.2.0](https://github.com/ngageoint/mage-android/releases/tag/5.2.0) (01-09-2017)

##### Features
* Users can now favorite observations
* Users with event edit permission can flag observations as important
* Added number of features to static layer list item.

##### Bug Fixes

## [5.1.1](https://github.com/ngageoint/mage-android/releases/tag/5.1.1) (11-10-2016)

##### Features

##### Bug Fixes
* Media picked from gallery stored in wrong location causing attachment failure in some cases.


## [5.1.0](https://github.com/ngageoint/mage-android/releases/tag/5.1.0) (08-11-2016)

##### Features
* Updated time filter intervals.
* Multi select support.
* Filter select and multi select options.

##### Bug Fixes
* Fix issue with duplicate attachments showing up in observation when picked from google photos gallery.
* Copy attachments picked from gallery to mage local storage in case gallery image is deleted before mage has
  a chance to send the attachment to the server.

## [5.0.4](https://github.com/ngageoint/mage-android/releases/tag/5.0.4) (05-25-2016)
##### Bug Fixes
* Fix issue with location marker anchor.  At higher zoom levels the marker was appearing in the wrong location
* Fixed issue with progress for feature overlay not being removed when the layer has been refreshed.
* Fixed issue on older devices with app crashing after launching camera app from new observation and then discarding the observation.

## [5.0.3](https://github.com/ngageoint/mage-android/releases/tag/5.0.3) (04-15-2016)
##### Features
* Number field support. Requires version 4.2+ of @ngageoint/mage-server

## [5.0.2](https://github.com/ngageoint/mage-android/releases/tag/5.0.2) (03-25-2016)
##### Bug Fixes
* Fix bug with user signup

## [5.0.1](https://github.com/ngageoint/mage-android/releases/tag/5.0.1) (12-04-2015)
##### Bug Fixes
* Hide/show local and google authentication sections correctly.
* Pull in latest MAGE SDK that removes api preferences on server URL switch
* Remove error on url text if switched to a valid server

## [5.0.0](https://github.com/ngageoint/mage-android/releases/tag/5.0.0) (11-23-2015)
##### Features
* Google oauth support through new MAGE server apis.
