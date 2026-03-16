package com.myoffgridai.library.repository;

import com.myoffgridai.library.model.ZimFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ZimFile} entities.
 */
@Repository
public interface ZimFileRepository extends JpaRepository<ZimFile, UUID> {

    /**
     * Finds a ZIM file by its unique filename.
     *
     * @param filename the filename to search for
     * @return the matching ZIM file, if present
     */
    Optional<ZimFile> findByFilename(String filename);

    /**
     * Retrieves all ZIM files ordered by display name ascending.
     *
     * @return all ZIM files sorted alphabetically by display name
     */
    List<ZimFile> findAllByOrderByDisplayNameAsc();

    /**
     * Checks whether a ZIM file with the given filename exists.
     *
     * @param filename the filename to check
     * @return true if a ZIM file with this filename exists
     */
    boolean existsByFilename(String filename);
}
