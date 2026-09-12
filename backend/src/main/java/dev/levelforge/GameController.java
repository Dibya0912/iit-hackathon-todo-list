package dev.levelforge;
import dev.levelforge.Dtos.*;
import dev.levelforge.Models.Category;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import javax.sql.DataSource;
import java.util.*;
@RestController @RequestMapping("/api")
public class GameController {
    private final GameService game; private final DataSource db;
    GameController(GameService game,DataSource db){this.game=game;this.db=db;}
    private long user(Authentication a){return Long.parseLong(a.getName());}
    @GetMapping("/health") ResponseEntity<Map<String,String>> health(){try(var c=db.getConnection()){return ResponseEntity.status(c.isValid(2)?200:503).body(Map.of("status",c.isValid(2)?"ready":"unavailable"));}catch(Exception e){return ResponseEntity.status(503).body(Map.of("status","unavailable"));}}
    @GetMapping("/character") HeroView character(Authentication a){return game.character(user(a));}
    @PatchMapping("/profile") HeroView profile(Authentication a,@Valid @RequestBody Profile p){return game.profile(user(a),p);}
    @GetMapping("/tasks") PageView<Quest> quests(Authentication a,@RequestParam(defaultValue="pending") String status,@RequestParam(required=false) Category category,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return game.quests(user(a),status,category,sort,page,size);}
    @PostMapping("/tasks") @ResponseStatus(HttpStatus.CREATED) Quest create(Authentication a,@Valid @RequestBody QuestInput q){return game.create(user(a),q);}
    @GetMapping("/tasks/{id}") Quest quest(Authentication a,@PathVariable long id){return game.quest(user(a),id);}
    @PatchMapping("/tasks/{id}") Quest edit(Authentication a,@PathVariable long id,@Valid @RequestBody QuestInput q){return game.edit(user(a),id,q);}
    @DeleteMapping("/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void archive(Authentication a,@PathVariable long id){game.archive(user(a),id);}
    @PostMapping("/tasks/{id}/complete") CompletionResult complete(Authentication a,@PathVariable long id){return game.complete(user(a),id);}
    @GetMapping("/shop") List<ItemView> shop(Authentication a){return game.shop(user(a));}
    @PostMapping("/shop/{id}/purchase") PurchaseResult buy(Authentication a,@PathVariable long id){return game.purchase(user(a),id);}
    @GetMapping("/inventory") List<ItemView> inventory(Authentication a){return game.inventory(user(a));}
    @PatchMapping("/inventory/{id}/equip") HeroView equip(Authentication a,@PathVariable long id){return game.equip(user(a),id,true);}
    @PatchMapping("/inventory/{id}/unequip") HeroView unequip(Authentication a,@PathVariable long id){return game.equip(user(a),id,false);}
    @GetMapping("/activity") PageView<Activity> activity(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return game.activity(user(a),page,size);}
}

