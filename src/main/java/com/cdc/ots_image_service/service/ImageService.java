package com.cdc.ots_image_service.service;

import com.cdc.ots_image_service.entity.Image;
import com.cdc.ots_image_service.repository.ImageRepository;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final Storage storage;

    @Value("${gcs.bucket.raw-images}")
    private String rawImagesBucket;

    @Value("${gcs.bucket.thumbnails}")
    private String thumbnailsBucket;

    public ImageService(ImageRepository imageRepository, Storage storage) {
        this.imageRepository = imageRepository;
        this.storage = storage;
    }

    public List<Image> getImagesByUser(String email) {
        return imageRepository.findAllByUploaderEmail(email);
    }

    public Image uploadImage(MultipartFile file, String email) throws IOException {
        String gcsPath = UUID.randomUUID() + "-" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(rawImagesBucket, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
        storage.create(blobInfo, file.getBytes());

        Image image = new Image();
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setGcsPath(gcsPath);
        image.setUploaderEmail(email);
        image.setThumbnailStatus(Image.ThumbnailStatus.PENDING);

        return imageRepository.save(image);
    }

    @Transactional
    public void deleteImageById(Long id, String email) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + id));

        if (!image.getUploaderEmail().equals(email)) {
            throw new AccessDeniedException("User does not have permission to delete this image");
        }

        deleteFromGcs(image);
        imageRepository.delete(image);
    }

    @Transactional
    public void deleteAllImagesByUser(String email) {
        List<Image> images = imageRepository.findAllByUploaderEmail(email);
        if (images.isEmpty()) {
            return;
        }

        images.forEach(this::deleteFromGcs);
        imageRepository.deleteAll(images);
    }

    private void deleteFromGcs(Image image) {
        // Delete raw image
        BlobId rawBlobId = BlobId.of(rawImagesBucket, image.getGcsPath());
        storage.delete(rawBlobId);

        // Delete thumbnail if it exists
        if (image.getThumbnailStatus() == Image.ThumbnailStatus.DONE) {
            // The thumbnail path is derived from the raw path
            String thumbnailGcsPath = "thumb-" + image.getGcsPath();
            BlobId thumbnailBlobId = BlobId.of(thumbnailsBucket, thumbnailGcsPath);
            storage.delete(thumbnailBlobId);
        }
    }
}