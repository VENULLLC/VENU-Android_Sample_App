package com.lrsus.venusdkjavasample;

import com.lrsus.venusdk.VENURange;

import java.util.UUID;

public class MyBrandRange extends VENURange {
    @Override
    public String appNamespace() {
        return MainApplication.APP_NAMESPACE;
    }

    @Override
    public UUID brandId() {
        return MainApplication.BRAND_ID;
    }
}
