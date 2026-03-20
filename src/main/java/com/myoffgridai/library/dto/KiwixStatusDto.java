package com.myoffgridai.library.dto;

/**
 * Status information for the Kiwix content server.
 *
 * @param available       whether the Kiwix server is reachable
 * @param url             the Kiwix server base URL
 * @param bookCount       the number of ZIM files loaded in Kiwix
 * @param processManaged  whether the server manages the kiwix-serve process
 */
public record KiwixStatusDto(
        boolean available,
        String url,
        int bookCount,
        boolean processManaged
) {
}
