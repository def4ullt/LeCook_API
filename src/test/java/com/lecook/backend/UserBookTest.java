package com.lecook.backend;

import com.lecook.backend.service.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@SpringBootTest
@Transactional
class UserBookTest extends IsolatedDatabase {
    @Autowired UserBookService books;
    @Autowired ProfilePhotos photos;
    @Autowired ProfileIdentity identity;
    @Autowired JdbcTemplate db;
    private UserBookService.OwnRecipe recipe(String id) {
        return new UserBookService.OwnRecipe(id,"Тестовий омлет","Приватний опис","Сніданки",150,true,2,10,
            List.of(new UserBookService.Ingredient("Яйце куряче","2","шт.")),List.of("Посмажте яйця."),"","",12.0,10.0,1.0);
    }
    @Test void usersAreIsolatedAndStaleWritesAreRejected() {
        String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString();
        var book=new UserBookService.Book(List.of(recipe(UUID.randomUUID().toString())),List.of("remote:favorite"),Map.of("diet","Без м’яса"));
        assertEquals(0,books.read(a).revision());
        var saved=books.save(a,new UserBookService.Update(0,book));
        assertEquals(1,saved.revision());assertEquals(book,saved.book());
        assertTrue(books.read(b).book().recipes().isEmpty());
        var e=assertThrows(ResponseStatusException.class,()->books.save(a,new UserBookService.Update(0,new UserBookService.Book(List.of(),List.of(),Map.of()))));
        assertEquals(409,e.getStatusCode().value());assertEquals(book,books.read(a).book());
        assertThrows(ResponseStatusException.class,()->books.publish(b,book.recipes().get(0).id()));
    }
    @Test void publishingCreatesOneStructuredPublicCopyAndKeepsThePrivateOriginal() {
        String user=UUID.randomUUID().toString(),local=UUID.randomUUID().toString();
        db.update("INSERT INTO categories(name) VALUES (?)","Сніданки");
        books.save(user,new UserBookService.Update(0,new UserBookService.Book(List.of(recipe(local)),List.of(),Map.of())));
        UUID id=books.publish(user,local);
        assertEquals(id,books.publish(user,local));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recipe_ingredients WHERE recipe_id=?",Integer.class,id));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recipe_steps WHERE recipe_id=?",Integer.class,id));
        assertEquals(12.0,db.queryForObject("SELECT protein FROM recipes WHERE id=?",Double.class,id));
        assertEquals(1,books.read(user).book().recipes().size());
    }
    @Test void privatePhotosRequireTheirOwnerAndRejectInvalidFiles() throws Exception {
        var image=new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"jpg",output);
        String ref=photos.put("owner",output.toByteArray());
        assertEquals(ref,photos.put("owner",output.toByteArray()));
        assertArrayEquals(output.toByteArray(),photos.get("owner",ref.substring(14)));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->photos.get("stranger",ref.substring(14))).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->photos.put("owner",new byte[]{1,2,3}));
    }
    @Test void missingUserSessionCannotAuthenticate() {
        assertEquals(401,assertThrows(ResponseStatusException.class,()->identity.require(null)).getStatusCode().value());
    }
}
