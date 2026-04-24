package ca.corbett.movienight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static boolean localhostWarningIssued = false;

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
    public AuthorizationManager<RequestAuthorizationContext> localhostOnlyAccess(
            @Value("${movienight.admin.localhost-only:true}") boolean localhostOnly) {
        return (authentication, context) -> {
            if (!localhostOnly) {
                emitLocalhostWarning();
                return new AuthorizationDecision(true);
            }
            return new AuthorizationDecision(isLoopbackAddress(context.getRequest().getRemoteAddr()));
        };
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
            logger.warn("Could not resolve remote address {}", remoteAddress, e);
            return false;
        }
    }

    /**
     * The first time this is invoked, it will emit a short warning to the log
     * about localhost-only access being disabled on the Admin API.
     * The warning is only emitted once per application run.
     */
    private void emitLocalhostWarning() {
        if (!localhostWarningIssued) {
            logger.warn("WARNING: movienight.admin.localhost-only is disabled. " +
                                "This allows admin access from any IP address, which may be a security risk. ");
            localhostWarningIssued = true;
        }
    }
}
