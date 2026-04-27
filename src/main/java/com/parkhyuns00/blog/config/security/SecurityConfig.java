package com.parkhyuns00.blog.config.security;

import com.parkhyuns00.blog.config.security.filter.AdminKeyAuthenticationFilter;
import com.parkhyuns00.blog.config.security.filter.AdminOtpAuthenticationFilter;
import com.parkhyuns00.blog.config.security.handler.AdminAuthenticationFailureHandler;
import com.parkhyuns00.blog.config.security.handler.AdminAuthenticationSuccessHandler;
import com.parkhyuns00.blog.config.security.provider.AdminKeyAuthenticationProvider;
import com.parkhyuns00.blog.config.security.provider.AdminOtpAuthenticationProvider;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final AdminKeyAuthenticationProvider adminKeyAuthenticationProvider;
    private final AdminOtpAuthenticationProvider adminOtpAuthenticationProvider;
    private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;
    private final AdminAuthenticationFailureHandler adminAuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        AuthenticationManager am = authenticationManager();
        SecurityContextRepository repository = securityContextRepository();

        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);

        http.sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        );

        http.securityContext(securityContext -> securityContext
            .securityContextRepository(repository)
        );

        http.authorizeHttpRequests((auth) ->  auth
            .requestMatchers(HttpMethod.POST, "/api/admin/auth/key").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/admin/auth/otp").hasRole(AdminRole.PRE_ADMIN.name())
            .requestMatchers("/api/admin/**").hasRole(AdminRole.ADMIN.name())
            .anyRequest().authenticated()
        );

        http.addFilterAfter(keyAuthenticationFilter(am, repository), LogoutFilter.class);
        http.addFilterAfter(otpAuthenticationFilter(am, repository), AdminKeyAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(
            adminKeyAuthenticationProvider,
            adminOtpAuthenticationProvider
        ));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private AdminKeyAuthenticationFilter keyAuthenticationFilter(AuthenticationManager am, SecurityContextRepository repo) {
        AdminKeyAuthenticationFilter filter = new AdminKeyAuthenticationFilter(am, objectMapper);
        filter.setAuthenticationSuccessHandler(adminAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(adminAuthenticationFailureHandler);
        filter.setSecurityContextRepository(repo);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        return filter;
    }

    private AdminOtpAuthenticationFilter otpAuthenticationFilter(AuthenticationManager am, SecurityContextRepository repo) {
        AdminOtpAuthenticationFilter filter = new AdminOtpAuthenticationFilter(am, objectMapper);
        filter.setAuthenticationSuccessHandler(adminAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(adminAuthenticationFailureHandler);
        filter.setSecurityContextRepository(repo);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy());
        return filter;
    }
}
