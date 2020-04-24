package com.inledco.exoterra.aliot;

import java.util.HashMap;
import java.util.Map;

public class ADevice {
    protected String productKey;
    protected String deviceName;

    private final Map<String, BaseProperty> items = new HashMap<>();

    public String getTag() {
        return productKey + "_" + deviceName;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Map<String, BaseProperty> getItems() {
        return items;
    }

    public void updateProperty(String key, BaseProperty property) {
        if (items.containsKey(key)) {
            BaseProperty prop = items.get(key);
            if (prop != null && property != null && prop.time < property.time) {
                property.setUpdated(true);
                items.put(key, property);
            }
        } else {
            property.setUpdated(true);
            items.put(key, property);
        }
    }

    public void updateProperties(Map<String, BaseProperty> properties) {
        for (String key : properties.keySet()) {
            BaseProperty property = properties.get(key);
            updateProperty(key, property);
        }
    }

    public Object getPropertyValue(String key) {
        if (items.containsKey(key)) {
            BaseProperty prop = items.get(key);
            if (prop != null) {
                prop.setUpdated(false);
                return prop.getValue();
            }
        }
        return null;
    }

    public long getPropertyTime(String key) {
        if (items.containsKey(key)) {
            BaseProperty prop = items.get(key);
            if (prop != null) {
                return prop.getTime();
            }
        }
        return 0;
    }
}
