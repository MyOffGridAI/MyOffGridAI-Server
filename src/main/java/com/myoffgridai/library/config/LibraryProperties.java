package com.myoffgridai.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the offline library system.
 *
 * <p>Binds properties under the {@code app.library} prefix including
 * directory paths for ZIM and eBook storage, external service URLs
 * (Kiwix, Calibre, Gutendex), and upload limits.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.library")
public class LibraryProperties {

    private String zimDirectory;
    private String ebookDirectory;
    private String kiwixUrl;
    private String calibreContentServerUrl;
    private String gutenbergApiUrl;
    private int maxUploadSizeMb;

    /** @return local directory path for ZIM file storage */
    public String getZimDirectory() {
        return zimDirectory;
    }

    public void setZimDirectory(String zimDirectory) {
        this.zimDirectory = zimDirectory;
    }

    /** @return local directory path for eBook file storage */
    public String getEbookDirectory() {
        return ebookDirectory;
    }

    public void setEbookDirectory(String ebookDirectory) {
        this.ebookDirectory = ebookDirectory;
    }

    /** @return base URL for the Kiwix content server */
    public String getKiwixUrl() {
        return kiwixUrl;
    }

    public void setKiwixUrl(String kiwixUrl) {
        this.kiwixUrl = kiwixUrl;
    }

    /** @return base URL for the Calibre content server */
    public String getCalibreContentServerUrl() {
        return calibreContentServerUrl;
    }

    public void setCalibreContentServerUrl(String calibreContentServerUrl) {
        this.calibreContentServerUrl = calibreContentServerUrl;
    }

    /** @return base URL for the Gutendex (Project Gutenberg) API */
    public String getGutenbergApiUrl() {
        return gutenbergApiUrl;
    }

    public void setGutenbergApiUrl(String gutenbergApiUrl) {
        this.gutenbergApiUrl = gutenbergApiUrl;
    }

    /** @return maximum upload size in megabytes */
    public int getMaxUploadSizeMb() {
        return maxUploadSizeMb;
    }

    public void setMaxUploadSizeMb(int maxUploadSizeMb) {
        this.maxUploadSizeMb = maxUploadSizeMb;
    }
}
