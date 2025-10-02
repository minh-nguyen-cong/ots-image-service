package com.cdc.ots_image_service.controller;

import com.cdc.ots_image_service.entity.Image;
import com.cdc.ots_image_service.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<List<Image>> getImagesForUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(imageService.getImagesByUser(email));
    }

    @PostMapping("/upload")
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        String email = authentication.getName();
        Image savedImage = imageService.uploadImage(file, email);
        return ResponseEntity.ok(savedImage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImageById(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        imageService.deleteImageById(id, email);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllImages(Authentication authentication) {
        String email = authentication.getName();
        imageService.deleteAllImagesByUser(email);
        return ResponseEntity.noContent().build();
    }
}
