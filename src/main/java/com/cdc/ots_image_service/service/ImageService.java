package com.cdc.ots_image_service.service;

import com.cdc.ots_image_service.entity.Image;
import com.cdc.ots_image_service.repository.ImageRepository;
import com.google.cloud.eventarc.publishing.v1.EventarcPublisherClient;
import com.google.cloud.eventarc.publishing.v1.PublishEventsRequest;
import com.google.cloud.eventarc.publishing.v1.PublishEventsResponse;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.cloudevents.v1.proto.CloudEvent.CloudEventAttributeValue;
import io.cloudevents.v1.proto.CloudEvent.Builder;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final Storage storage;
    private final EventarcPublisherClient eventarcPublisherClient;
    private final String eventarcChannel;

    @Value("${gcs.bucket.raw-images}")
    private String rawImagesBucket;

    public ImageService(
            ImageRepository imageRepository,
            Storage storage,
            @Value("${gcp.eventarc.project-id}") String projectId,
            @Value("${gcp.eventarc.location}") String location,
            @Value("${gcp.eventarc.channel}") String channel) throws IOException {
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.eventarcPublisherClient = EventarcPublisherClient.create();
        this.eventarcChannel = String.format("projects/%s/locations/%s/channels/%s", projectId, location, channel);
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
        Builder eventBuilder = io.cloudevents.v1.proto.CloudEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setSource(URI.create("image-service").toString())
                .setSpecVersion("1.0")
                .setType("com.cdc.image.uploaded");

        eventBuilder.putAttributes("datacontenttype",
                CloudEventAttributeValue.newBuilder().setCeString("application/json").build());

        eventBuilder.setProtoData(Any.newBuilder().setValue(ByteString.copyFromUtf8(savedImage.getId().toString())).build());

        PublishEventsRequest request = PublishEventsRequest.newBuilder()
                .setChannel(eventarcChannel)
                .addEvents(eventBuilder.build())
                .build();

        PublishEventsResponse response = this.eventarcPublisherClient.publishEvents(request);

        return savedImage;
    }

    public List<Image> getImagesByUploader(String uploaderEmail) {
        return imageRepository.findByUploaderEmail(uploaderEmail);
    }
    
    public Optional<Image> getImageByIdAndUploader(Long id, String uploaderEmail) {
        return imageRepository.findByIdAndUploaderEmail(id, uploaderEmail);
    }
}
