package com.myoffgridai.system.dto;

/**
 * Represents the current WiFi connection status of the device.
 *
 * @param connected   whether the device is connected to a WiFi network
 * @param hasInternet whether the device has internet connectivity
 */
public record WifiConnectionStatus(boolean connected, boolean hasInternet) {
}
