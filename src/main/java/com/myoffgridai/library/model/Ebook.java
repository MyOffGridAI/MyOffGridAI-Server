package com.myoffgridai.library.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an eBook in the offline library.
 *
 * <p>Supports multiple formats (EPUB, PDF, MOBI, AZW, TXT, HTML).
 * Books can be uploaded directly or imported from Project Gutenberg.</p>
 */
@Entity
@Table(name = "ebooks", indexes = {
        @Index(name = "idx_ebooks_gutenberg_id", columnList = "gutenberg_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Ebook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(length = 2000, columnDefinition = "TEXT")
    private String description;

    private String isbn;

    private String publisher;

    @Column(name = "published_year")
    private Integer publishedYear;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EbookFormat format;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "cover_image_path")
    private String coverImagePath;

    @Column(name = "gutenberg_id")
    private String gutenbergId;

    @Column(name = "download_count")
    private int downloadCount;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }

    // ── Getters and Setters ─────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public EbookFormat getFormat() {
        return format;
    }

    public void setFormat(EbookFormat format) {
        this.format = format;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public String getGutenbergId() {
        return gutenbergId;
    }

    public void setGutenbergId(String gutenbergId) {
        this.gutenbergId = gutenbergId;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
