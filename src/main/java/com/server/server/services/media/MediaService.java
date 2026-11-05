package com.server.server.services.media;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.constants.report.ReportRules;
import com.server.server.dto.common.PageResponse;
import com.server.server.enums.media.MediaCategory;
import com.server.server.enums.report.ReportFormat;
import com.server.server.enums.report.ReportType;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.models.media.Media;
import com.server.server.models.user.User;
import com.server.server.repositories.media.MediaRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.common.FileStorageService;
import com.server.server.utilities.pagination.PaginationUtils;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.annotation.PostConstruct;
import com.server.server.config.properties.AppProperties;
import com.server.server.services.common.SystemSchedulingService;

/** Stored files and who may see them: the owner, or an administrator. */
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    private final AccessPolicyService accessPolicy;
    private final SystemSchedulingService schedulingService;
    private final AppProperties appProperties;

    @PostConstruct
    void registerCleanup() {
        schedulingService.registerNightlyTask(this::deleteExpiredReports);
    }

    /** Nightly: removes reports older than {@code app.report-retention-days}, files and records. */
    @Transactional
    public int deleteExpiredReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(appProperties.reportRetentionDays());
        List<Media> expired = mediaRepository.findByCategoryAndCreatedAtBefore(MediaCategory.REPORT, cutoff);
        expired.forEach(media -> fileStorageService.deletePrivate(media.getStoragePath()));
        mediaRepository.deleteAll(expired);
        return expired.size();
    }

    /** Saves a generated report for its owner under {@code reports/<ownerId>/<random>.<ext>}. */
    @Transactional
    public Media storeReport(byte[] content, String fileName, ReportType type, ReportFormat format, int recordCount, User owner) {
        String storagePath = ReportRules.MEDIA_REPORT_FOLDER + "/" + owner.getId() + "/" + UUID.randomUUID() + "." + format.extension();
        fileStorageService.writePrivate(storagePath, content);
        Media media = new Media();
        media.setFileName(fileName);
        media.setContentType(format.contentType());
        media.setSizeBytes(content.length);
        media.setStoragePath(storagePath);
        media.setCategory(MediaCategory.REPORT);
        media.setOwner(owner);
        media.setReportType(type);
        media.setReportFormat(format);
        media.setRecordCount(recordCount);
        return mediaRepository.save(media);
    }

    /** The signed-in user's files, newest first, optionally of one category. */
    @Transactional(readOnly = true)
    public PageResponse<Media> listMine(MediaCategory category, Integer page, Integer size) {
        Integer ownerId = accessPolicy.requireCurrentUser().getId();
        var pageable = PaginationUtils.pageable(page, size, "createdAt", "desc", "createdAt", Set.of("createdAt"));
        return PageResponse.from(category == null ? mediaRepository.findByOwnerId(ownerId, pageable)
                : mediaRepository.findByOwnerIdAndCategory(ownerId, category, pageable));
    }

    @Transactional(readOnly = true)
    public MediaFile open(@NonNull Integer id) {
        Media media = getVisible(id);
        return new MediaFile(media.getFileName(), media.getContentType(), fileStorageService.readPrivate(media.getStoragePath()));
    }

    @Transactional
    public void delete(@NonNull Integer id) {
        Media media = getVisible(id);
        fileStorageService.deletePrivate(media.getStoragePath());
        mediaRepository.delete(media);
    }

    private Media getVisible(Integer id) {
        Media media = mediaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Media", id));
        User current = accessPolicy.requireCurrentUser();
        if (!AccessPolicyService.isAdmin(current) && !Objects.equals(media.getOwner().getId(), current.getId())) {
            throw new AccessDeniedException("You can only access your own files.");
        }
        return media;
    }
}
