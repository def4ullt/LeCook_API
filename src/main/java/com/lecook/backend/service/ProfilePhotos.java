package com.lecook.backend.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

@Service
public class ProfilePhotos {
    private final JdbcTemplate db;
    public ProfilePhotos(JdbcTemplate db) { this.db=db; }
    public String put(String user,byte[] bytes) {
        try {
            if(bytes.length==0||bytes.length>1048576) throw new IllegalArgumentException();
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(input); if(!readers.hasNext()) throw new IllegalArgumentException();
                var reader=readers.next();
                try { reader.setInput(input); if(!reader.getFormatName().equalsIgnoreCase("jpeg") || reader.getWidth(0)>2048 || reader.getHeight(0)>2048) throw new IllegalArgumentException(); }
                finally { reader.dispose(); }
            }
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if(db.queryForObject("SELECT COUNT(*) FROM profile_photos WHERE user_id=?",Integer.class,user)>=100 && db.queryForObject("SELECT COUNT(*) FROM profile_photos WHERE user_id=? AND hash=?",Integer.class,user,hash)==0)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Ліміт: 100 власних фото на профіль");
            db.update("INSERT INTO profile_photos(user_id,hash,bytes) VALUES(?,?,?) ON CONFLICT DO NOTHING",user,hash,bytes);
            return "profile-photo:"+hash;
        } catch(ResponseStatusException e) { throw e; } catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Потрібне JPEG-фото до 1 МБ та 2048 пікселів"); }
    }
    public byte[] get(String user,String hash) {
        if(!hash.matches("[0-9a-f]{64}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var rows=db.query("SELECT bytes FROM profile_photos WHERE user_id=? AND hash=?",(rs,n)->rs.getBytes(1),user,hash);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Фото не знайдено");
        return rows.get(0);
    }
}
