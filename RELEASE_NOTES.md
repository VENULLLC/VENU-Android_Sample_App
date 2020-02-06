## 1.1.0-beta
## SDK
* Enabled SSL through PubNub, matching iOS configuration.
* Enabled PubNub reconnection policy, also matching iOS configuration.
* Added onDeviceConnected callback in Interface VENUCallback to determine when user's device is back online.
* Mapped VENUError.CONNECTION_ERROR to issues related to PubNub handshake timeout, which may be indicative of poor network service.
* Mapped issues when the user's mobile phone disconencts to VENUError.DEVICE_CONNECTED

## Sample App
* Show the broadcast status and device connection status more explicitly in the UI rather than as a Toast.
* Added app icon.
* Show service number in UI
* Added missing activity transitions between MainActivity and OrderActivity screen.

## 1.0.2-beta
## SDK
* Fixed issue related to PubNub channel name character restriction for iOS and Android.

## 1.0.0
## SDK
* Transition from Firebase to utilizing PubNub for communication with VEN-U controller
* Deprecated VENUManager.serviceNumber(brandId, siteId, serviceChangeListener) -> VENUManager.startServiceNumber()
* Deprecated VENUManager.cancelServiceNumber(brandId, siteId, serviceChangeListener) -> VENUManager.clearServiceNumber()
* Deprecated VENUManager.startServiceOrder(brandId, siteId, serviceChangeListener) -> VENUManager.startServiceOrder()
* Deprecated VENUManager.checkForServiceNumber() -> VENUManager.serviceStatus()
* Site GUID is now to be set when calling VENUManager.start(guid: UUID)

## 0.3.4
## SDK
* Fixed issues related to service number not expiring correctly and not being released to pool

## 0.0.1
## SDK
Initial alpha.
