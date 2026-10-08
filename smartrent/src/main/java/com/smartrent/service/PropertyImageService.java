package com.smartrent.service;

import com.smartrent.dto.PropertyImageResponse;
import com.smartrent.entity.Property;
import com.smartrent.entity.PropertyImage;
import com.smartrent.entity.User;
import com.smartrent.repository.PropertyImageRepository;
import com.smartrent.repository.PropertyRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PropertyImageService {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGES = 6;
    private static final int MAX_INPUT_SIDE = 8000;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final int MAX_OUTPUT_SIDE = 1920;

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository imageRepository;
    private final CurrentUserService currentUserService;

    public PropertyImageService(
            PropertyRepository propertyRepository,
            PropertyImageRepository imageRepository,
            CurrentUserService currentUserService) {

        this.propertyRepository = propertyRepository;
        this.imageRepository = imageRepository;
        this.currentUserService = currentUserService;
    }

    public PropertyImageResponse upload(
            Long propertyId,
            MultipartFile file) {

        Property property = requireManagedProperty(propertyId, true);

        if (imageRepository.countByPropertyId(propertyId) >= MAX_IMAGES) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A property can have at most 6 photos"
            );
        }

        ValidatedImage validated = validateAndConvert(file);

        PropertyImage image = new PropertyImage();
        image.setPropertyId(propertyId);
        image.setContentType("image/jpeg");
        image.setImageData(validated.data());
        image.setByteSize(validated.data().length);
        image.setWidth(validated.width());
        image.setHeight(validated.height());
        image.setSortOrder(
                imageRepository.findMaximumSortOrder(propertyId) + 1
        );

        // A changed gallery must be reviewed before public display.
        property.setVerificationStatus("PENDING");

        PropertyImage saved = imageRepository.saveAndFlush(image);
        return toResponse(saved);
    }

    public void delete(Long propertyId, Long imageId) {
        Property property = requireManagedProperty(propertyId, true);
        PropertyImage image = findImage(propertyId, imageId);

        imageRepository.delete(image);
        property.setVerificationStatus("PENDING");
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> listPublic(Long propertyId) {
        requirePublicProperty(propertyId);
        return imageRepository.findMetadataByPropertyId(propertyId);
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> listManaged(Long propertyId) {
        requireManagedProperty(propertyId, false);
        return imageRepository.findMetadataByPropertyId(propertyId);
    }

    @Transactional(readOnly = true)
    public ImageContent readPublic(Long propertyId, Long imageId) {
        requirePublicProperty(propertyId);
        return toContent(findImage(propertyId, imageId));
    }

    @Transactional(readOnly = true)
    public ImageContent readManaged(Long propertyId, Long imageId) {
        requireManagedProperty(propertyId, false);
        return toContent(findImage(propertyId, imageId));
    }

    private Property requirePublicProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> notFound("Property not found"));

        if (!"VERIFIED".equals(property.getVerificationStatus())
                || property.getOwnerId() == null) {
            throw notFound("Property not found");
        }

        return property;
    }

    private Property requireManagedProperty(
            Long propertyId,
            boolean lock) {

        User user = currentUserService.requireOwnerOrAdmin();

        Property property = (
                lock
                        ? propertyRepository.findByIdForUpdate(propertyId)
                        : propertyRepository.findById(propertyId)
        ).orElseThrow(() -> notFound("Property not found"));

        boolean admin = "ADMIN".equals(user.getRole());
        boolean owner = user.getId().equals(property.getOwnerId());

        if (!admin && !owner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only manage photos for your own properties"
            );
        }

        return property;
    }

    private PropertyImage findImage(Long propertyId, Long imageId) {
        return imageRepository.findByIdAndPropertyId(imageId, propertyId)
                .orElseThrow(() -> notFound("Photo not found"));
    }

    private ValidatedImage validateAndConvert(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Choose a photo to upload");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw badRequest("Each photo must be 5 MB or smaller");
        }

        try (
                InputStream input = file.getInputStream();
                MemoryCacheImageInputStream imageInput =
                        new MemoryCacheImageInputStream(input)
        ) {
            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw badRequest("Upload a valid JPEG or PNG image");
            }

            ImageReader reader = readers.next();

            try {
                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                if (!format.equals("jpeg")
                        && !format.equals("jpg")
                        && !format.equals("png")) {
                    throw badRequest("Only JPEG and PNG photos are supported");
                }

                reader.setInput(imageInput, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0
                        || height <= 0
                        || width > MAX_INPUT_SIDE
                        || height > MAX_INPUT_SIDE
                        || (long) width * height > MAX_PIXELS) {
                    throw badRequest(
                            "Photo must be at most 8000 pixels per side "
                                    + "and 20 megapixels in total"
                    );
                }

                BufferedImage original = reader.read(0);

                if (original == null) {
                    throw badRequest("The photo could not be decoded");
                }

                try {
                    return convertToJpeg(original);
                } finally {
                    original.flush();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw badRequest(
                    "The photo could not be processed. Try another JPEG or PNG."
            );
        }
    }

    private ValidatedImage convertToJpeg(
            BufferedImage original) throws IOException {

        double scale = Math.min(
                1.0,
                (double) MAX_OUTPUT_SIDE
                        / Math.max(original.getWidth(), original.getHeight())
        );

        int width = Math.max(
                1,
                (int) Math.round(original.getWidth() * scale)
        );

        int height = Math.max(
                1,
                (int) Math.round(original.getHeight() * scale)
        );

        BufferedImage converted = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );

        try {
            Graphics2D graphics = converted.createGraphics();

            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);

                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );

                graphics.drawImage(original, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(converted, "jpeg", output)) {
                    throw new IOException("JPEG encoder unavailable");
                }

                byte[] bytes = output.toByteArray();

                if (bytes.length > MAX_FILE_BYTES) {
                    throw badRequest(
                            "The processed photo is too large. Use a smaller image."
                    );
                }

                return new ValidatedImage(bytes, width, height);
            }
        } finally {
            converted.flush();
        }
    }

    private PropertyImageResponse toResponse(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getPropertyId(),
                image.getContentType(),
                image.getByteSize(),
                image.getWidth(),
                image.getHeight(),
                image.getSortOrder(),
                image.getCreatedAt()
        );
    }

    private ImageContent toContent(PropertyImage image) {
        return new ImageContent(
                image.getContentType(),
                image.getImageData()
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private record ValidatedImage(
            byte[] data,
            int width,
            int height
    ) {
    }

    public record ImageContent(
            String contentType,
            byte[] data
    ) {
    }
}