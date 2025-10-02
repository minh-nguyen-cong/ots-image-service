package com.cdc.ots_image_service.repository;

import com.cdc.ots_image_service.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findAllByUploaderEmail(String uploaderEmail);
}