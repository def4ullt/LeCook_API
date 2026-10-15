package com.lecook.backend;

import com.lecook.backend.config.CatalogPhotos;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class CatalogPhotosTest {
    @Test void everyCatalogPhotoHasAttributionAndImmutableFilename() {
        assertEquals(90,CatalogPhotos.ALL.size());
        var names=new HashSet<String>();
        for(var photo:CatalogPhotos.ALL) {
            assertTrue(names.add(photo.name()));
            assertTrue(photo.filename().matches("[a-f0-9]{24}\\.(jpg|png)"));
            assertFalse(photo.credit().isBlank());
            assertTrue(photo.source().startsWith("https://commons.wikimedia.org/wiki/File:"));
            assertTrue(java.util.Set.of("thumb.wikimedia.org", "upload.wikimedia.org").contains(java.net.URI.create(photo.original()).getHost()));
        }
    }
}
