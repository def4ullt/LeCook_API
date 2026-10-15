package com.lecook.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.math.BigDecimal;

@Service
public class UserBookService {
    public record Ingredient(String name,String quantity,String unit) {}
    public record OwnRecipe(String id,String name,String description,String category,int calories,boolean caloriesKnown,int servings,int preparationMinutes,List<Ingredient> ingredients,List<String> steps,String imageUrl,String imageCredit,Double protein,Double fat,Double carbs) {}
    public record Book(List<OwnRecipe> recipes,List<String> favorites,Map<String,String> preferences) {}
    public record Version(long revision,Book book) {}
    public record Update(long revision,Book book) {}
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final ProfilePhotos photos;
    private final org.springframework.beans.factory.ObjectProvider<RecipeImageService> images;
    public UserBookService(JdbcTemplate db,ObjectMapper json,ProfilePhotos photos,org.springframework.beans.factory.ObjectProvider<RecipeImageService> images) { this.db=db; this.json=json;this.photos=photos;this.images=images; }
    private Book empty() { return new Book(List.of(),List.of(),Map.of()); }
    @Transactional public Version read(String user) {
        db.update("INSERT INTO user_books(user_id) VALUES (?) ON CONFLICT DO NOTHING",user);
        return db.queryForObject("SELECT revision,data FROM user_books WHERE user_id=?",(rs,n)-> {
            try { String s=rs.getString(2); return new Version(rs.getLong(1),s.equals("{}")?empty():json.readValue(s,Book.class)); }
            catch(Exception e) { throw new IllegalStateException("Invalid stored book"); }
        },user);
    }
    public void validate(Book b) {
        if(b==null || b.recipes()==null || b.favorites()==null || b.preferences()==null || b.recipes().size()>500 || b.favorites().size()>10000 || b.preferences().size()>20) bad();
        var ids=new HashSet<String>();
        for(var r:b.recipes()) {
            if(r==null || r.id()==null || !r.id().matches("[0-9a-f-]{36}") || !ids.add(r.id()) || !text(r.name(),1,120) || !text(r.description(),0,20000) || !text(r.category(),1,60) || r.servings()<1 || r.servings()>1000 || r.preparationMinutes()<1 || r.preparationMinutes()>10080 || r.calories()<0 || r.calories()>100000 || r.ingredients()==null || r.ingredients().isEmpty() || r.ingredients().size()>100 || r.steps()==null || r.steps().isEmpty() || r.steps().size()>100 || !text(r.imageCredit(),0,2000)) bad();
            if(r.imageUrl()!=null && !r.imageUrl().isEmpty() && !r.imageUrl().matches("profile-photo:[0-9a-f]{64}") && (!r.imageUrl().startsWith("https://") || r.imageUrl().length()>2000)) bad();
            var names=new HashSet<String>();
            for(var i:r.ingredients()) {
                if(i==null || !text(i.name(),1,120) || !names.add(i.name().trim().toLowerCase(Locale.ROOT)) || !Set.of("г","мл","шт.","ч. л.","ст. л.").contains(i.unit())) bad();
                try { var q=new BigDecimal(i.quantity()); if(q.signum()<=0 || q.compareTo(new BigDecimal("9999999999.99"))>0 || q.stripTrailingZeros().scale()>2) bad(); } catch(Exception e) { bad(); }
            }
            if(r.steps().stream().anyMatch(s->!text(s,1,4000))) bad();
            for(Double v:Arrays.asList(r.protein(),r.fat(),r.carbs())) if(v!=null && (!Double.isFinite(v)||v<0||v>10000)) bad();
        }
        if(b.favorites().stream().anyMatch(s->!text(s,1,300)) || b.preferences().entrySet().stream().anyMatch(e->!text(e.getKey(),1,80)||!text(e.getValue(),0,500))) bad();
        try { if(json.writeValueAsBytes(b).length>4_000_000) bad(); } catch(Exception e) { bad(); }
    }
    private boolean text(String s,int min,int max) { return s!=null && s.trim().length()>=min && s.length()<=max; }
    private void bad() { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Перевірте склад, кроки та розмір книги"); }
    @Transactional public Version save(String user,Update update) {
        validate(update.book()); read(user);
        try {
            int changed=db.update("UPDATE user_books SET data=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND revision=?",json.writeValueAsString(update.book()),user,update.revision());
            if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Профіль оновлено на іншому пристрої. Спочатку завантажте його зміни");
            return read(user);
        } catch(ResponseStatusException e) { throw e; } catch(Exception e) { throw new IllegalStateException("Could not save book"); }
    }
    /** Publishing creates an immutable public copy; subsequent private edits stay private. */
    @Transactional public UUID publish(String user,String localId) {
        read(user);
        db.queryForObject("SELECT revision FROM user_books WHERE user_id=? FOR UPDATE",Long.class,user);
        var r=read(user).book().recipes().stream().filter(x->x.id().equals(localId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Спочатку синхронізуйте власний рецепт"));
        var prior=db.queryForList("SELECT recipe_id FROM user_publications WHERE user_id=? AND local_id=?",UUID.class,user,UUID.fromString(localId));
        if(!prior.isEmpty()) return prior.get(0);
        if(db.queryForObject("SELECT COUNT(*) FROM user_publications WHERE user_id=?",Integer.class,user)>=100) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Ліміт: 100 публікацій на профіль");
        db.execute("LOCK TABLE categories IN SHARE ROW EXCLUSIVE MODE");
        var category=db.queryForList("SELECT id FROM categories WHERE name=?",Integer.class,r.category());
        if(category.isEmpty()) category=List.of(db.queryForObject("INSERT INTO categories(name) VALUES (?) RETURNING id",Integer.class,r.category()));
        UUID id=UUID.randomUUID();
        String url=r.imageUrl();
        if(url!=null && url.startsWith("profile-photo:")) {
            try { url=images.getObject().upload(photos.get(user,url.substring(14))).url(); }
            catch(java.io.IOException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Не вдалося опублікувати фото"); }
        }
        db.update("INSERT INTO recipes(id,name,description,category_id,calories,image_url,servings,preparation_minutes,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",id,r.name(),r.description(),category.get(0),r.caloriesKnown()?r.calories():null,url,r.servings(),r.preparationMinutes());
        db.update("UPDATE recipes SET protein=?,fat=?,carbs=? WHERE id=?",r.protein(),r.fat(),r.carbs(),id);
        for(var i:r.ingredients()) {
            // Serialize name lookup/insertion to preserve the shared M–M ingredient dictionary.
            db.execute("LOCK TABLE ingredients IN SHARE ROW EXCLUSIVE MODE");
            var found=db.queryForList("SELECT id FROM ingredients WHERE lower(name)=lower(?) ORDER BY id LIMIT 1",Integer.class,i.name());
            int ingredient;
            if(found.isEmpty()) ingredient=db.queryForObject("INSERT INTO ingredients(name) VALUES (?) RETURNING id",Integer.class,i.name()); else ingredient=found.get(0);
            db.update("INSERT INTO recipe_ingredients(recipe_id,ingredient_id,quantity,unit) VALUES(?,?,?,?)",id,ingredient,new BigDecimal(i.quantity()),i.unit());
        }
        for(int i=0;i<r.steps().size();i++) db.update("INSERT INTO recipe_steps(recipe_id,step_number,instruction) VALUES(?,?,?)",id,i+1,r.steps().get(i));
        db.update("INSERT INTO user_publications(user_id,local_id,recipe_id) VALUES(?,?,?)",user,UUID.fromString(localId),id);
        return id;
    }
}
