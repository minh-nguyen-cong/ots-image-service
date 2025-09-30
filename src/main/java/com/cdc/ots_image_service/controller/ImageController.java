package com.cdc.ots_image_service.controller;

import com.cdc.ots_image_service.entity.Image;
import com.cdc.ots_image_service.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/upload")
    public ResponseEntity<Image> uploadImage(@RequestParam("file") MultipartFile file,
                                             @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        Image savedImage = imageService.uploadImage(file, userDetails.getUsername());
        return ResponseEntity.ok(savedImage);
    }

    @GetMapping
    public ResponseEntity<List<Image>> getAllImagesForUser(@AuthenticationPrincipal UserDetails userDetails) {
        List<Image> images = imageService.getImagesByUploader(userDetails.getUsername());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Image> getImageById(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return imageService.getImageByIdAndUploader(id, userDetails.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
