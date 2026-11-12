package com.myapp.aqdeeha.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

/**
 * Local-disk storage.
 * <ul>
 *   <li>{@code public/...}  – profile / vehicle photos, served read-only under {@code app.storage.public-url-prefix}</li>
 *   <li>{@code private/...} – ID-card images, AES-256-GCM encrypted ({@code [12-byte IV][ciphertext+tag]}), never served</li>
 * </ul>
 * Stored values are storage keys relative to {@code app.storage.root}, e.g. {@code public/users/<uuid>.jpg}.
 */
@Slf4j
@Service
public class FileStorageService {

    public static final String PUBLIC_DIR = "public";
    private static final String PRIVATE_DIR = "private";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final Path root;
    private final SecretKeySpec idCardKey;
    private final String publicUrlPrefix;
    private final SecureRandom random = new SecureRandom();

    public FileStorageService(StorageProperties properties) {
        this.root = Path.of(properties.root()).toAbsolutePath().normalize();
        byte[] key = Base64.getDecoder().decode(properties.idCardKey());
        if (key.length != 32) {
            throw new IllegalStateException("app.storage.id-card-key must be a base64-encoded 32-byte key");
        }
        this.idCardKey = new SecretKeySpec(key, "AES");
        this.publicUrlPrefix = properties.publicUrlPrefix();
    }

    public Path root() {
        return root;
    }

    /** Stores an image readable by anyone holding its URL. */
    public String storePublic(MultipartFile file, ImageValidator.ImageType type, String folder) {
        String key = PUBLIC_DIR + "/" + folder + "/" + UUID.randomUUID() + "." + type.extension();
        write(key, readBytes(file));
        return key;
    }

    /** Stores an image encrypted at rest under the private area. */
    public String storeEncrypted(MultipartFile file, ImageValidator.ImageType type, String folder) {
        String key = PRIVATE_DIR + "/" + folder + "/" + UUID.randomUUID() + "." + type.extension() + ".enc";
        write(key, encrypt(readBytes(file)));
        return key;
    }

    /** Decrypts a private file (for the future admin review endpoint). */
    public byte[] readEncrypted(String key) {
        try {
            return decrypt(Files.readAllBytes(resolve(key)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Image media type from a stored key, e.g. {@code .../x.png.enc} -> {@code image/png}. */
    public static String mediaTypeOf(String key) {
        String k = key.endsWith(".enc") ? key.substring(0, key.length() - 4) : key;
        if (k.endsWith(".png")) {
            return "image/png";
        }
        if (k.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /** Public URL for a {@code public/...} key; {@code null} for null input. */
    public String publicUrl(String key) {
        if (key == null) {
            return null;
        }
        return publicUrlPrefix + key.substring(PUBLIC_DIR.length());
    }

    /** Best-effort cleanup used when a DB write fails after files were stored. */
    public void deleteQuietly(Collection<String> keys) {
        for (String key : keys) {
            try {
                Files.deleteIfExists(resolve(key));
            } catch (Exception e) {
                log.warn("Could not delete stored file {}", key, e);
            }
        }
    }

    private void write(String key, byte[] bytes) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, idCardKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    private byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, idCardKey, new GCMParameterSpec(GCM_TAG_BITS, data, 0, GCM_IV_BYTES));
            return cipher.doFinal(data, GCM_IV_BYTES, data.length - GCM_IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
