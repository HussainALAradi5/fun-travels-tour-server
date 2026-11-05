package com.server.server.repositories.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.media.MediaCategory;
import com.server.server.models.media.Media;
import java.time.LocalDateTime;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Integer> {

    Page<Media> findByOwnerId(Integer ownerId, Pageable pageable);

    Page<Media> findByOwnerIdAndCategory(Integer ownerId, MediaCategory category, Pageable pageable);

    List<Media> findByCategoryAndCreatedAtBefore(MediaCategory category, LocalDateTime cutoff);
}
