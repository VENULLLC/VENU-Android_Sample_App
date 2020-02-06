# VENU SDK Java Sample App

## Directory Contents
This project folder can be opened by Android Studio.

* venusdkjavasample/: Android Java sample
* docs/: Javadocs

## Build notes
* For usage in Java, it is required to add `implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"` to your gradle build dependencies. See build.gradle for example.
* SDK artifact is published in Azure and requires authentication to pull. Set your authentication token as an environment variable `AZURE_ARTIFACTS_ENV_ACCESS_TOKEN` or as `azureArtifactsGradleAccessToken` as part of `gradle.properties`
* You must set the BRAND_ID, APP_SECRET, LOCATION GUID of the site to connect with here: `venusdkjavasample/src/defaultVenu/res/values/venu.xml`

### Manifest merging
When using gradle, manifestmerging should be enabled. After importing the project into Android Studio, open `gradle.properties` and add:

```
manifestmerger.enabled=true
```

This will ensure that the permissions required for the library services are added correctly.

## Application notes

### Permissions
In order for the library to perform BLE ranging, Android requires at minimum `COURSE_LOCATION` permissions. Therefore, the required location request permission flow should be added to the application so the user can allow usage of the services.

## Source notes

### Android Pie Background Scanning Restrictions
With Android Pie, monitoring for the associated BRAND_ID is restricted to 15 minutes in the background. To enable monitoring immediately, it can be done in the foreground:

```java
VENUMonitor.getInstance(this).setForeground(true);
```

### Service Expiration
Within ``VENUCallback``, ``onServiceExpiration`` will be called after service number has expired. If there is nothing specific, an expired service number can be referred to how services are cleared.

```java
@Override
public void onServiceExpiration(VENUServiceNumber serviceNumber) {
    onServiceCleared(serviceNumber);
}
```
