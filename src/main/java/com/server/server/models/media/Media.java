package com.server.server.models.media;

import com.server.server.enums.media.MediaCategory;
import com.server.server.enums.report.ReportFormat;
import com.server.server.enums.report.ReportType;
import com.server.server.models.base.BaseEntity;
import com.server.server.models.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * A stored file: where it lives under {@code app.media-dir}, what it is, and who owns it. Files are never
 * served statically; they are downloaded through the access-checked media endpoint.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "media")
public class Media extends BaseEntity {

    /** Name offered to the browser on download. */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Path relative to the media folder; never exposed to clients. */
    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 40)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_format", length = 10)
    private ReportFormat reportFormat;

    @Column(name = "record_count")
    private Integer recordCount;
}
