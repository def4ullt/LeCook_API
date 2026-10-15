package com.lecook.backend.controller;

import com.lecook.backend.service.ProfileIdentity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/account/auth")
public class ProfileAuthController {
    private final ProfileIdentity identity;
    public ProfileAuthController(ProfileIdentity identity) { this.identity=identity; }
    public record Credentials(@NotBlank @Size(max=255) String email,@NotBlank @Size(min=8,max=128) String password,@Size(max=80) String name) {}
    @PostMapping("/signup") public ResponseEntity<ProfileIdentity.Login> signup(@Valid @RequestBody Credentials c) { return login(c,true); }
    @PostMapping("/login") public ResponseEntity<ProfileIdentity.Login> signin(@Valid @RequestBody Credentials c) { return login(c,false); }
    private ResponseEntity<ProfileIdentity.Login> login(Credentials c, boolean signup) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identity.login(signup,c.email(),c.password(),c.name()==null?"":c.name())); }
    @GetMapping("/session") public ProfileIdentity.User session(@RequestHeader(value="Authorization",required=false) String h) { return identity.require(h); }
    @PostMapping("/logout") public void logout(@RequestHeader(value="Authorization",required=false) String h) { identity.logout(h); }
}
