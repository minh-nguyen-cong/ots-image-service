package com.cdc.ots_image_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cdc.ots_image_service.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUploaderEmail(String uploaderEmail);
    Optional<Image> findByIdAndUploaderEmail(Long id, String uploaderEmail);
}