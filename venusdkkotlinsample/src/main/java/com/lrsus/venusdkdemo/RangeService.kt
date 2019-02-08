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
class RangeService : VENURange() {

    override fun appNamespace(): String {
        return MainApplication.APP_NAMESPACE
    }

    override fun brandId(): UUID {
//        return UUID.fromString("f05b1f5a-646e-4169-b0c6-a50b84c8b624")
        return UUID.fromString("671d3a8e-ee94-4395-9177-d5382d75ff10")
    }
}