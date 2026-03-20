package com.myoffgridai.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Kiwix process management system.
 *
 * <p>Binds properties under the {@code app.kiwix} prefix including
 * the binary path, port, thread count, and catalog base URL.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.kiwix")
public class KiwixProperties {

    private boolean enabled;
    private String binaryPath = "/opt/homebrew/bin/kiwix-serve";
    private int port = 8888;
    private int timeoutSeconds = 15;
    private int threads = 4;
    private boolean manageProcess = true;
    private String catalogBaseUrl = "https://library.kiwix.org";

    /** @return whether kiwix process management is enabled */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return path to the kiwix-serve binary */
    public String getBinaryPath() {
        return binaryPath;
    }

    public void setBinaryPath(String binaryPath) {
        this.binaryPath = binaryPath;
    }

    /** @return port kiwix-serve listens on */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /** @return startup health check timeout in seconds */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** @return number of threads for kiwix-serve */
    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    /** @return whether the server should manage the kiwix-serve process */
    public boolean isManageProcess() {
        return manageProcess;
    }

    public void setManageProcess(boolean manageProcess) {
        this.manageProcess = manageProcess;
    }

    /** @return base URL for the Kiwix online catalog (OPDS) */
    public String getCatalogBaseUrl() {
        return catalogBaseUrl;
    }

    public void setCatalogBaseUrl(String catalogBaseUrl) {
        this.catalogBaseUrl = catalogBaseUrl;
    }
}
