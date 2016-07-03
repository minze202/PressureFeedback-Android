/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.compressionfeedback.hci.pressurefeedback;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String PRESSURE_SERVICE = "00771312-1100-0000-0000-abba0fa1afe1";
    public static String READABLE_PRESSURE_CHARACTERISTIC = "00694203-0077-1210-1342-abba0fa1afe1";
    public static String WRITABLE_PRESSURE_PATTERN_CHARACTERISTIC = "00704204-0077-1210-1342-abba0fa1afe1";
    public static String WRITABLE_PRESSURE_STRENGTH_CHARACTERISTIC = "00714204-0077-1210-1342-abba0fa1afe1";
    public static String READABLE_PRESSURE_VALUE_CHARACTERISTIC = "00724204-0077-1210-1342-abba0fa1afe1";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(PRESSURE_SERVICE, "Pressure Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(READABLE_PRESSURE_CHARACTERISTIC, "Readable pressure pattern characteristic");
        attributes.put(WRITABLE_PRESSURE_PATTERN_CHARACTERISTIC, "Writable pressure pattern characteristic");
        attributes.put(WRITABLE_PRESSURE_STRENGTH_CHARACTERISTIC, "Writable pressure strength characteristic");
        attributes.put(READABLE_PRESSURE_VALUE_CHARACTERISTIC, "Readable pressure value characteristic");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
