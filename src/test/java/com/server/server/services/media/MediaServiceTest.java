package com.server.server.services.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.server.server.config.properties.AppProperties;
import com.server.server.enums.media.MediaCategory;
import com.server.server.models.media.Media;
import com.server.server.repositories.media.MediaRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.common.FileStorageService;
import com.server.server.services.common.SystemSchedulingService;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AccessPolicyService accessPolicy;
    @Mock
    private SystemSchedulingService schedulingService;
    @Mock
    private AppProperties appProperties;
    @InjectMocks
    private MediaService mediaService;

    @Test
    void expiredReportsLoseTheirFilesAndRecords() {
        Media old = new Media();
        old.setStoragePath("reports/5/old.pdf");
        when(appProperties.reportRetentionDays()).thenReturn(30);
        when(mediaRepository.findByCategoryAndCreatedAtBefore(eq(MediaCategory.REPORT), any())).thenReturn(List.of(old));

        assertThat(mediaService.deleteExpiredReports()).isEqualTo(1);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mediaRepository).findByCategoryAndCreatedAtBefore(eq(MediaCategory.REPORT), cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(LocalDateTime.now().minusDays(30).minusMinutes(1), LocalDateTime.now().minusDays(30).plusMinutes(1));
        verify(fileStorageService).deletePrivate("reports/5/old.pdf");
        verify(mediaRepository).deleteAll(List.of(old));
    }
}
