package dev.levelforge;
import dev.levelforge.Models.*;
import dev.levelforge.Repositories.*;
import dev.levelforge.Dtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
@Service
public class AuthService {
    private final Users users; private final Heroes heroes; private final Attributes attributes; private final PasswordEncoder passwords;
    private final String dummyHash;
    AuthService(Users users,Heroes heroes,Attributes attributes,PasswordEncoder passwords){this.users=users;this.heroes=heroes;this.attributes=attributes;this.passwords=passwords;dummyHash=passwords.encode("dummy-password-for-timing");}
    static String email(String value){return value.trim().toLowerCase(Locale.ROOT);}
    @Transactional public UserView signup(Signup input) {
        Rules.zone(input.timezone());
        if(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ApiError(400,"PASSWORD_LENGTH","Password must be at most 72 UTF-8 bytes.");
        if(users.findByEmail(email(input.email())).isPresent()) throw new ApiError(409,"SIGNUP_UNAVAILABLE","Unable to create this account. Try signing in.");
        User u=new User();u.email=email(input.email());u.displayName=input.displayName().trim();u.passwordHash=passwords.encode(input.password());users.saveAndFlush(u);
        Hero h=new Hero();h.userId=u.id;h.timezone=input.timezone();heroes.saveAndFlush(h);
        for(String type:List.of("INTELLECT","STRENGTH","WISDOM","FOCUS","CHARISMA")){Attribute a=new Attribute();a.characterId=h.id;a.type=type;attributes.save(a);}
        return view(u);
    }
    public UserView login(Login input) {
        var found=users.findByEmail(email(input.email()));
        boolean valid=passwords.matches(input.password(),found.map(u->u.passwordHash).orElse(dummyHash));
        if(!valid || found.isEmpty()) throw new ApiError(401,"LOGIN_FAILED","Email or password is incorrect.");
        return view(found.get());
    }
    public UserView me(long id){return view(users.findById(id).orElseThrow(ApiError::missing));}
    static UserView view(User u){return new UserView(u.id,u.displayName,u.email);}
}

