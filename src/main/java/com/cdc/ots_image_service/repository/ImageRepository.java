package com.cdc.ots_image_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cdc.ots_image_service.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {
 
}