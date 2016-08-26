# Change Log
All notable changes to this project will be documented in this file.
Adheres to [Semantic Versioning](http://semver.org/).

---
## 5.2.1 (TBD)

* TBD

##### Features

##### Bug Fixes

## [5.2.0](https://github.com/ngageoint/mage-android/releases/tag/5.2.0) (01-09-2017)

##### Features
* Users can now favorite observations
* Users with event edit permission can flag observations as important
* Users can share observations with other applications on the device, ie email.
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
