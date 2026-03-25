package com.myoffgridai.library.repository;

import com.myoffgridai.library.model.Ebook;
import com.myoffgridai.library.model.EbookFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Ebook} entities.
 */
@Repository
public interface EbookRepository extends JpaRepository<Ebook, UUID> {

    /**
     * Searches ebooks by title/author with optional format filter.
     *
     * <p>When {@code search} is null or blank, all ebooks are returned.
     * When {@code format} is null, no format filter is applied.</p>
     *
     * @param search the search term for title or author (case-insensitive)
     * @param format the format filter (nullable)
     * @param pageable pagination parameters
     * @return a page of matching ebooks
     */
    @Query("SELECT e FROM Ebook e WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.author) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:format IS NULL OR e.format = :format) " +
           "ORDER BY e.title ASC")
    Page<Ebook> searchByTitleOrAuthor(
            @Param("search") String search,
            @Param("format") EbookFormat format,
            Pageable pageable);

    /**
     * Returns all non-null Gutenberg IDs from the ebooks table.
     *
     * @return list of Gutenberg IDs already imported
     */
    @Query("SELECT e.gutenbergId FROM Ebook e WHERE e.gutenbergId IS NOT NULL")
    List<String> findAllGutenbergIds();

    /**
     * Finds an ebook by its Project Gutenberg ID.
     *
     * @param gutenbergId the Gutenberg book ID
     * @return the matching ebook, if present
     */
    Optional<Ebook> findByGutenbergId(String gutenbergId);

    /**
     * Checks whether an ebook with the given Gutenberg ID already exists.
     *
     * @param gutenbergId the Gutenberg book ID
     * @return true if an ebook with this Gutenberg ID exists
     */
    boolean existsByGutenbergId(String gutenbergId);
}
