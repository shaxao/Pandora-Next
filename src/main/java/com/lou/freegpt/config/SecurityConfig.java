package com.lou.freegpt.config;

import com.lou.freegpt.handler.LoginFailHandle;
import com.lou.freegpt.handler.LoginSuccessHandle;
import com.lou.freegpt.handler.MyAccessDeniedHandler;
import com.lou.freegpt.mapper.ChatUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private UserDetailsService userDetailsService;


    @Autowired
    private PersistentTokenRepository repository;

    private CorsConfig corsConfig;

    public SecurityConfig (CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }




    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.disable());
                //.cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configure(http));
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(PERMITS_LIST.toArray(new String[0])).permitAll()
//                        .anyRequest().authenticated()
//                );
        http.exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
            httpSecurityExceptionHandlingConfigurer.accessDeniedHandler(new MyAccessDeniedHandler());
        });
        http.logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer
                .clearAuthentication(true)
                .invalidateHttpSession(true));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(authenticationProvider);
    }

//    private static final String[] PERMITS = new String[]{"/login","/login.html", "/api/user/register", "/api/user/username", "/api/user/email/code","/api/user/phone","/api/user/email"};
//    public static final List<String> PERMITS_LIST;
//
//    static {
//        PERMITS_LIST = Arrays.asList(PERMITS);
//    }
//public static void main(String[] args) {
//    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//    String encode = passwordEncoder.encode("Lw2000314..");
//    System.out.println(encode);
//}
}
