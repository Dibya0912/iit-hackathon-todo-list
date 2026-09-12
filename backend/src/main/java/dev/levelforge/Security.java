package dev.levelforge;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
@Configuration
public class Security {
    @Bean java.time.Clock clock(){return java.time.Clock.systemUTC();}
    @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultLogin() {
        return username -> { throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use the session authentication API."); };
    }
    @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder(12);}
    @Bean SecurityContextRepository contexts(){return new HttpSessionSecurityContextRepository();}
    @Bean CsrfTokenRepository csrfTokens(){return new HttpSessionCsrfTokenRepository();}
    @Bean SecurityFilterChain chain(HttpSecurity http,SecurityContextRepository contexts,CsrfTokenRepository csrf) throws Exception {
        return http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/signup","/api/auth/login","/api/health").permitAll().anyRequest().authenticated())
            .csrf(c->c.csrfTokenRepository(csrf))
            .securityContext(c->c.securityContextRepository(contexts))
            .requestCache(c->c.disable())
            .formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Please sign in to continue.\",\"fields\":{}}");})
            .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Your security token expired. Please retry.\",\"fields\":{}}");}))
            .build();
    }
}
