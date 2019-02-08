# VENU SDK and Java Sample

## Change log
* 11/16/2018 - Initial alpha.
* 11/19/2018 - Addition of initial check to `enteredVENULocation`. Java version of demo added along with generated javadocs.

## Directory Contents
This project folder can be opened by Android Studio.

* venusdkjavasample: Android Java sample
* venusdkkotlinsample: Android Kotlin sample
* libs: SDK release aars
* docs: Javadocs

## Build notes
For usage in Java, it is required to add `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"` to your gradle build dependencies. See build.gradle for example.

### Manifest merging
When using gradle, manifestmerging should be enabled. After importing the project into Android Studio, open `gradle.properties` and add:

```
manifestmerger.enabled=true
```

This will ensure that the permissions required for the library services are added correctly.

## Application notes

### Permissions
In order for the library to perform BLE ranging, Android requires at minimum `COURSE_LOCATION` permissions. Therefore, the required location request permission flow should be added to the application so the user can allow usage of the services.