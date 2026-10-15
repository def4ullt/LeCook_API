package com.lecook.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.URI;
import java.util.*;

@Service
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class RecipeImageService {
    private final S3Client client;
    private final String bucket;
    private final String endpoint;
    public RecipeImageService(S3Client client, Environment environment) {
        this.client = client;
        bucket = environment.getProperty("LECOOK_STORAGE_BUCKET", "lecook-recipe-images");
        endpoint = environment.getRequiredProperty("AWS_ENDPOINT_URL_S3").replaceAll("/+$", "");
    }
    public record UploadedImage(String key, String url, String contentType, int bytes) {}

    public UploadedImage upload(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > 4 * 1024 * 1024) throw new IllegalArgumentException("Image must be between 1 byte and 4 MiB");
        String format;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Expected a JPEG or PNG image");
            var reader = readers.next();
            try {
                reader.setInput(input);
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg").contains(format)) throw new IllegalArgumentException("Expected a JPEG or PNG image");
                if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) throw new IllegalArgumentException("Image dimensions must not exceed 4096 pixels");
            } finally { reader.dispose(); }
        } catch (IOException error) { throw new IllegalArgumentException("Invalid JPEG or PNG image"); }
        boolean png = format.equals("png");
        String type = png ? "image/png" : "image/jpeg";
        String key = "recipe-images/" + UUID.randomUUID() + (png ? ".png" : ".jpg");
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type)
            .cacheControl("public, max-age=31536000, immutable").build(), RequestBody.fromBytes(bytes));
        return new UploadedImage(key, URI.create(endpoint + "/" + bucket + "/" + key).toString(), type, bytes.length);
    }
    public byte[] download(String key) { return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray(); }
    public void delete(String key) {
        if (!key.matches("recipe-images/[0-9a-f-]{36}\\.(png|jpg)")) throw new IllegalArgumentException("Invalid managed image key");
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
