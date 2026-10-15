package com.lecook.backend.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Native clients hold an opaque signed session cookie, never a provider/admin credential. */
@Service
public class ProfileIdentity {
    private final Environment env;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private long window; private int calls;
    public ProfileIdentity(Environment env, ObjectMapper json) { this.env=env; this.json=json; }
    private String base() {
        String value=env.getProperty("NEON_AUTH_BASE_URL", env.getProperty("lecook.auth-base-url", "")).replaceAll("/+$", "");
        if(!value.matches("https://[a-z0-9.-]+\\.neon\\.tech/[^\\s?#]+")) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Авторизація ще не налаштована на сервері");
        return value;
    }
    public record User(String id, String name, String email) {}
    public record Login(String token, User user) {}
    private User user(JsonNode n) { return new User(n.path("id").asText(),n.path("name").asText(),n.path("email").asText()); }
    private String cookie(String token) {
        if(token==null || !token.matches("[a-zA-Z0-9%._~-]{20,2048}")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Увійдіть у профіль");
        return "__Secure-neon-auth.session_token="+token;
    }
    private HttpResponse<byte[]> request(String path, String token, Object body) {
        try {
            var b=HttpRequest.newBuilder(URI.create(base()+path)).timeout(Duration.ofSeconds(25))
                .header("Origin",env.getProperty("LECOOK_AUTH_ORIGIN","https://lecook-api.onrender.com"));
            if(token!=null) b.header("Cookie",cookie(token));
            if(body==null) b.GET(); else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            var r=http.send(b.build(),HttpResponse.BodyHandlers.ofByteArray());
            if(r.body().length>100000) throw new IllegalStateException();
            return r;
        } catch(ResponseStatusException e) { throw e; }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Вхід тимчасово недоступний"); }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Сервіс авторизації тимчасово недоступний"); }
    }
    public synchronized Login login(boolean signup, String email, String password, String name) {
        long now=System.currentTimeMillis()/60000;
        if(now!=window) { window=now;calls=0; }
        if(++calls>20) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Забагато спроб входу. Зачекайте хвилину");
        if(email==null || !email.matches("[^\\s@]{1,120}@[^\\s@]{1,120}\\.[^\\s@]{2,30}") || password==null || password.length()<8 || password.length()>128 || name==null || name.length()>80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Перевірте email та пароль (8–128 символів)");
        var r=request(signup?"/sign-up/email":"/sign-in/email",null,Map.of("email",email.trim(),"password",password,"name",name.isBlank()?"Кулінар":name.trim()));
        if(r.statusCode()!=200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,signup?"Не вдалося зареєструватися. Перевірте дані або увійдіть в існуючий акаунт":"Не вдалося увійти. Перевірте email і пароль");
        try {
            String value=r.headers().allValues("set-cookie").stream().filter(c->c.startsWith("__Secure-neon-auth.session_token=")).map(c->c.substring(c.indexOf('=')+1).split(";",2)[0]).findFirst().orElseThrow();
            cookie(value);
            return new Login(value,user(json.readTree(r.body()).path("user")));
        } catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Не вдалося створити сесію"); }
    }
    public User require(String authorization) {
        String token=token(authorization);
        var r=request("/get-session",token,null);
        try {
            var n=json.readTree(r.body()).path("user");
            if(r.statusCode()!=200 || n.path("id").asText().isBlank()) throw new IllegalArgumentException();
            return user(n);
        } catch(Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Сесія завершилася. Увійдіть ще раз"); }
    }
    public String token(String header) { if(header==null || !header.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Увійдіть у профіль"); return header.substring(7); }
    public void logout(String header) { request("/sign-out",token(header),Map.of()); }
}
