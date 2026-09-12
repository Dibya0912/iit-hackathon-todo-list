package dev.levelforge;
import dev.levelforge.Dtos.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import java.util.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final SecurityContextRepository contexts; private final CsrfTokenRepository csrf;
    private final Map<String,Attempt> attempts=new LinkedHashMap<>();
    private record Attempt(int count,long start) {}
    AuthController(AuthService auth,SecurityContextRepository contexts,CsrfTokenRepository csrf){this.auth=auth;this.contexts=contexts;this.csrf=csrf;}
    private synchronized void throttle(String key) {
        long now=System.currentTimeMillis();
        attempts.entrySet().removeIf(e->now-e.getValue().start()>900000);
        Attempt a=attempts.getOrDefault(key,new Attempt(0,now));
        if(a.count()>=20 || attempts.size()>=10000 && !attempts.containsKey(key)) throw new ApiError(429,"RATE_LIMITED","Too many attempts. Try again in 15 minutes.");
        attempts.put(key,new Attempt(a.count()+1,a.start()));
    }
    @GetMapping("/csrf") public Map<String,String> token(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
    @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
    public UserView signup(@Valid @RequestBody Signup input,HttpServletRequest req,HttpServletResponse res){throttle(req.getRemoteAddr());UserView u=auth.signup(input);establish(u,req,res);return u;}
    @PostMapping("/login") public UserView login(@Valid @RequestBody Login input,HttpServletRequest req,HttpServletResponse res){throttle(req.getRemoteAddr());UserView u=auth.login(input);establish(u,req,res);return u;}
    private void establish(UserView u,HttpServletRequest req,HttpServletResponse res) {
        req.getSession();req.changeSessionId();
        SecurityContext context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(Long.toString(u.id()),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);csrf.saveToken(null,req,res);
    }
    @GetMapping("/me") public UserView me(Authentication authentication){return auth.me(Long.parseLong(authentication.getName()));}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpServletRequest req,HttpServletResponse res){
        var session=req.getSession(false);if(session!=null) session.invalidate();
        SecurityContextHolder.clearContext();
        Cookie cookie=new Cookie("SESSION","");cookie.setMaxAge(0);cookie.setPath("/");cookie.setHttpOnly(true);res.addCookie(cookie);
    }
}

