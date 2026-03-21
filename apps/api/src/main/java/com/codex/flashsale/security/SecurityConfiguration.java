package com.codex.flashsale.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/flash-sales/**", "/api/v1/reservations/**", "/api/v1/inventory/**", "/api/v1/orders/**")
                        .permitAll()
                        .requestMatchers("/api/v1/channel-ingress/tiktok/**").permitAll()
                        .requestMatchers("/api/v1/admin/auth/login", "/api/v1/admin/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/admin/campaigns/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/ops/**", "/api/v1/ops/**", "/api/v1/admin/auth/logout")
                        .hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "OPERATOR")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(this::convert)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    SecretKey secretKey(@Value("${app.security.jwt.secret}") String jwtSecret) {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Collection<GrantedAuthority> authorities = roles == null
                ? List.<GrantedAuthority>of()
                : roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("username"));
    }
}
