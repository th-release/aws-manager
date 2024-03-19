package com.threlease.base.utils.security;

import com.threlease.base.utils.jsonwebtoken.JwtAuthenticationFilter;
import com.threlease.base.utils.jsonwebtoken.JwtProvider;
import com.threlease.base.utils.responses.BasicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/aws/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling((exceptionConfig) ->
                        exceptionConfig.authenticationEntryPoint(
                                ((request, response, authException) -> {
                                    BasicResponse b_response = BasicResponse.builder()
                                            .success(false)
                                            .message(Optional.of("올바른 요청이 아닙니다."))
                                            .data(Optional.empty())
                                            .build();
                                    response.setContentType("application/json");
                                    response.setCharacterEncoding("utf-8");
                                    response.setStatus(400);
                                    response.getWriter().write(b_response.toJson());
                                })
                        ).accessDeniedHandler(((request, response, accessDeniedException) -> {
                            BasicResponse b_response = BasicResponse.builder()
                                    .success(false)
                                    .message(Optional.of("권한이 없는 사용자입니다."))
                                    .data(Optional.empty())
                                    .build();
                            response.setStatus(403);
                            response.setCharacterEncoding("utf-8");
                            response.setContentType("application/json");
                            response.getWriter().write(b_response.toJson());
                        }))
                );
        return http.build();
    }
}
