# Change Log
All notable changes to this project will be documented in this file.
Adheres to [Semantic Versioning](http://semver.org/).

---
## 6.2.2 (TBD)

* TBD

##### Features
* Update to Android SDK 28.
* Location service nows runs as a proper foreground service.
* Observations and attachments are being pulled and pushed via WorkManager.
* Updated okhttp, retrofit and glide dependencies.
* Using Dagger for dependency injection.
* GeoPackage features that contain links are clickable.

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
