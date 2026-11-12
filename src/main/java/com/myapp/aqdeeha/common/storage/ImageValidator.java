package com.myapp.aqdeeha.common.storage;

import com.myapp.aqdeeha.common.exception.BusinessException;
import com.myapp.aqdeeha.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Accepts only JPEG / PNG / WEBP images up to {@code app.storage.max-image-size}.
 * The type is detected from the file's magic bytes, not from the client's Content-Type or file name.
 */
@Component
@RequiredArgsConstructor
public class ImageValidator {

    private final StorageProperties properties;

    /** @return the detected type, or {@code null} when the optional file was not sent */
    public ImageType validate(MultipartFile file, String field, boolean required) {
        Object label = new DefaultMessageSourceResolvable("field." + field);
        if (file == null || file.isEmpty()) {
            if (required) {
                throw BusinessException.forField(ErrorCode.IMAGE_REQUIRED, field, label);
            }
            return null;
        }
        if (file.getSize() > properties.maxImageSize().toBytes()) {
            throw BusinessException.forField(ErrorCode.IMAGE_TOO_LARGE, field, label,
                    properties.maxImageSize().toMegabytes());
        }
        ImageType type = detect(file);
        if (type == null) {
            throw BusinessException.forField(ErrorCode.IMAGE_INVALID_TYPE, field, label);
        }
        return type;
    }

    private ImageType detect(MultipartFile file) {
        byte[] h = new byte[12];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            return null;
        }
        if (n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return ImageType.JPG;
        }
        if (n >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return ImageType.PNG;
        }
        if (n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return ImageType.WEBP;
        }
        return null;
    }

    public enum ImageType {
        JPG("jpg"), PNG("png"), WEBP("webp");

        private final String extension;

        ImageType(String extension) {
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }
    }
}
