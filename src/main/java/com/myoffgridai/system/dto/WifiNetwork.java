package com.myoffgridai.system.dto;

/**
 * Represents a WiFi network discovered during AP mode scanning.
 *
 * @param ssid           the network name
 * @param signalStrength the signal strength in dBm (higher is stronger)
 * @param security       the security type (e.g. "WPA2", "WPA3", "Open")
 */
public record WifiNetwork(String ssid, int signalStrength, String security) {
}
