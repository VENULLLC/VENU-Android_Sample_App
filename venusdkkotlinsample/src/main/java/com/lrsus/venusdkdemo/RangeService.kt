package com.lrsus.venusdkdemo

import com.lrsus.venusdk.VENURange
import java.util.*

/**
 * RangeService extends VENURange similarly like MainApplication extends VENUDiscoveryReceiver.
 * This is meant to be started once a location has been discovered in a wider sense.
 *
 * Once within range of a location, this would eventually offer a service number to which
 * VENUBroadcast can be used.
 */
class RangeService {

    fun appNamespace(): String {
        return MainApplication.APP_NAMESPACE
    }

    fun brandId(): UUID {
//        return UUID.fromString(getString(R.string.BRAND_ID))
        return UUID.randomUUID()
    }
}