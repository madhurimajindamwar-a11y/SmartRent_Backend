package com.smartrent.controller;

import com.smartrent.dto.PropertyImageResponse;
import com.smartrent.service.PropertyImageService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PropertyImageController {

    private final PropertyImageService imageService;

    public PropertyImageController(PropertyImageService imageService) {
        this.imageService = imageService;
    }

    // Public: list photos belonging to a verified property.
    @GetMapping("/properties/{propertyId}/images")
    public ResponseEntity<List<PropertyImageResponse>> listPublic(
            @PathVariable("propertyId") Long propertyId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(imageService.listPublic(propertyId));
    }

    // Public: display a photo belonging to a verified property.
    @GetMapping("/properties/{propertyId}/images/{imageId}/content")
    public ResponseEntity<byte[]> readPublic(
            @PathVariable("propertyId") Long propertyId,
            @PathVariable("imageId") Long imageId) {

        return imageResponse(
                imageService.readPublic(propertyId, imageId),
                imageId
        );
    }

    // Owner/admin: list photos, including those awaiting review.
    @GetMapping("/manage/properties/{propertyId}/images")
    public ResponseEntity<List<PropertyImageResponse>> listManaged(
            @PathVariable("propertyId") Long propertyId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(imageService.listManaged(propertyId));
    }

    // Owner/admin: display a photo, including one awaiting review.
    @GetMapping("/manage/properties/{propertyId}/images/{imageId}/content")
    public ResponseEntity<byte[]> readManaged(
            @PathVariable("propertyId") Long propertyId,
            @PathVariable("imageId") Long imageId) {

        return imageResponse(
                imageService.readManaged(propertyId, imageId),
                imageId
        );
    }

    // Owner/admin: upload one photo.
    @PostMapping(
            value = "/manage/properties/{propertyId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PropertyImageResponse> upload(
            @PathVariable("propertyId") Long propertyId,
            @RequestParam("file") MultipartFile file) {

        PropertyImageResponse saved =
                imageService.upload(propertyId, file);

        URI location = URI.create(
                "/api/manage/properties/"
                        + propertyId
                        + "/images/"
                        + saved.id()
                        + "/content"
        );

        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .body(saved);
    }

    // Owner/admin: delete one photo.
    @DeleteMapping("/manage/properties/{propertyId}/images/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable("propertyId") Long propertyId,
            @PathVariable("imageId") Long imageId) {

        imageService.delete(propertyId, imageId);

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> imageResponse(
            PropertyImageService.ImageContent content,
            Long imageId) {

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.data().length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"property-photo-" + imageId + ".jpg\""
                )
                .body(content.data());
    }
}