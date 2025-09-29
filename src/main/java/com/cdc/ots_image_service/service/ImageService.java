package com.cdc.ots_image_service.service;

import com.cdc.ots_image_service.entity.Image;
import com.cdc.ots_image_service.repository.ImageRepository;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final Storage storage;

    @Value("${gcs.bucket.raw-images}")
    private String rawImagesBucket;

    public ImageService(ImageRepository imageRepository, Storage storage) {
        this.imageRepository = imageRepository;
        this.storage = storage;
    }

    public Image uploadImage(MultipartFile file, String uploaderEmail) throws IOException {
        // 1. Generate a unique file name to avoid collisions
        String uniqueFileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        // 2. Upload to Google Cloud Storage
        BlobId blobId = BlobId.of(rawImagesBucket, uniqueFileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());

        // 3. Create and save metadata to Cloud SQL
        Image image = new Image();
        image.setFileName(file.getOriginalFilename());
        image.setFileSize(file.getSize());
        image.setGcsPath(uniqueFileName); // Store the unique name
        image.setUploaderEmail(uploaderEmail);
        image.setThumbnailStatus(Image.ThumbnailStatus.PENDING);

        Image savedImage = imageRepository.save(image);

        // 4. Publish a CloudEvent to Eventarc to trigger the thumbnail worker
        // CloudEvent event = CloudEventBuilder.v1()
        //         .withId(UUID.randomUUID().toString())
        //         .withSource(URI.create("image-service"))
        //         .withType("com.cdc.ots.image.uploaded")
        //         .withDataContentType("application/json")
        //         .withData(savedImage.getId().toString().getBytes())
        //         .build();

        // eventarcMessageSender.send(event);

        return savedImage;
    }

    public List<Image> getAllImages() {
        return imageRepository.findAll();
    }

    public Optional<Image> getImageById(Long id) {
        return imageRepository.findById(id);
    }
}
