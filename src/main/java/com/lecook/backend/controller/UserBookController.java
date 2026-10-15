package com.lecook.backend.controller;

import com.lecook.backend.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class UserBookController {
    private final ProfileIdentity identity;
    private final UserBookService books;
    private final ProfilePhotos photos;
    private final org.springframework.beans.factory.ObjectProvider<RecipeImageService> images;
    public UserBookController(ProfileIdentity identity,UserBookService books,ProfilePhotos photos,org.springframework.beans.factory.ObjectProvider<RecipeImageService> images) { this.identity=identity;this.books=books;this.photos=photos;this.images=images; }
    @GetMapping("/book") public ResponseEntity<UserBookService.Version> get(@RequestHeader(value="Authorization",required=false) String h) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(books.read(identity.require(h).id())); }
    @PutMapping("/book") public UserBookService.Version put(@RequestHeader(value="Authorization",required=false) String h,@RequestBody UserBookService.Update body) { return books.save(identity.require(h).id(),body); }
    @PostMapping("/publish/{id}") public Map<String,String> publish(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id) { return Map.of("id",books.publish(identity.require(h).id(),id).toString()); }
    @PostMapping(value="/photos",consumes="image/jpeg") public Map<String,String> upload(@RequestHeader(value="Authorization",required=false) String h,@RequestBody byte[] bytes) { return Map.of("imageUrl",photos.put(identity.require(h).id(),bytes)); }
    @GetMapping(value="/photos/{hash}",produces="image/jpeg") public ResponseEntity<byte[]> photo(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String hash) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(photos.get(identity.require(h).id(),hash)); }
}
