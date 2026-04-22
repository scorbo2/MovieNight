package ca.corbett.movienight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthorizationManager<RequestAuthorizationContext> localhostOnlyAccess
    ) throws Exception {
        AuthorizationManager<RequestAuthorizationContext> adminAccess =
                AuthorizationManagers.allOf(localhostOnlyAccess, hasRole("ADMIN"));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/frontend/**", "/favicon.svg", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/files").access(adminAccess)
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").access(adminAccess)
                        .requestMatchers(HttpMethod.POST, "/api/**").access(adminAccess)
                        .requestMatchers(HttpMethod.PUT, "/api/**").access(adminAccess)
                        .requestMatchers(HttpMethod.DELETE, "/api/**").access(adminAccess)
                        .anyRequest().denyAll()
                );

        return http.build();
    }

    @Bean
    public AuthorizationManager<RequestAuthorizationContext> localhostOnlyAccess() {
        return (authentication, context) ->
                new AuthorizationDecision(isLoopbackAddress(context.getRequest().getRemoteAddr()));
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${movienight.admin.username}") String username,
            @Value("${movienight.admin.password}") String password,
            PasswordEncoder passwordEncoder
    ) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles("ADMIN")
                        .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isLoopbackAddress(String remoteAddress) {
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
