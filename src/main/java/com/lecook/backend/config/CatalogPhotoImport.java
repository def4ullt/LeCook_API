package com.lecook.backend.config;

import java.nio.file.*;
import java.net.URI;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

/** Explicit import only. Uploads immutable objects, then atomically links existing recipes. */
@Component
@ConditionalOnProperty(name="lecook.catalog.photos")
public class CatalogPhotoImport implements ApplicationRunner {
    private final S3Client storage;
    private final JdbcTemplate db;
    private final Environment env;
    private final TransactionTemplate tx;
    public CatalogPhotoImport(S3Client storage, JdbcTemplate db, Environment env, PlatformTransactionManager manager) {
        this.storage=storage; this.db=db; this.env=env; this.tx=new TransactionTemplate(manager);
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        if (CatalogPhotos.ALL.size()!=90) throw new IllegalStateException("Expected photos for all 90 recipes");
        Path root=Path.of(env.getRequiredProperty("lecook.catalog.photos")).toAbsolutePath().normalize();
        String bucket=env.getProperty("LECOOK_STORAGE_BUCKET", "lecook-recipe-images");
        String endpoint=env.getRequiredProperty("AWS_ENDPOINT_URL_S3").replaceAll("/+$", "");
        var updates=new ArrayList<Object[]>();
        for (var photo : CatalogPhotos.ALL) {
            if (!photo.filename().matches("[a-f0-9]{24}\\.(jpg|png)")) throw new IllegalStateException("Invalid photo filename");
            Path file=root.resolve(photo.filename());
            byte[] bytes=Files.readAllBytes(file);
            if (bytes.length>2*1024*1024) throw new IllegalStateException("Photo too large");
            var image=ImageIO.read(file.toFile());
            if (image==null || image.getWidth()>2048 || image.getHeight()>2048) throw new IllegalStateException("Invalid photo");
            String key="catalog/v1/"+photo.filename();
            try { storage.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()); }
            catch (S3Exception error) {
                if (error.statusCode()!=404) throw error;
                storage.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(photo.filename().endsWith("png")?"image/png":"image/jpeg")
                    .cacheControl("public, max-age=31536000, immutable").build(), RequestBody.fromBytes(bytes));
            }
            String url=URI.create(endpoint+"/"+bucket+"/"+key).toString();
            UUID id=UUID.nameUUIDFromBytes(("lecook:catalog:uk:v1:"+photo.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            updates.add(new Object[]{url, id});
        }
        tx.executeWithoutResult(status -> {
            db.execute("SELECT pg_advisory_xact_lock(768213409521)");
            int[] counts=db.batchUpdate("UPDATE recipes SET image_url=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND (image_url IS NULL OR image_url<>?)", updates.stream().map(r -> new Object[]{r[0],r[1],r[0]}).toList());
            System.out.println("CATALOG PHOTOS: linked="+Arrays.stream(counts).sum()+", total="+CatalogPhotos.ALL.size());
        });
    }
}
