package com.myapp.aqdeeha.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @param root             base directory for uploaded files
 * @param idCardKey        base64 AES-256 key (32 bytes) used to encrypt ID-card images at rest
 * @param maxImageSize     max size of one uploaded image
 * @param publicUrlPrefix  URL prefix under which public images are served
 * @param signedUrlTtl     lifetime of signed links to private images (ID cards)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String root, String idCardKey, DataSize maxImageSize, String publicUrlPrefix,
                                java.time.Duration signedUrlTtl) {
}
