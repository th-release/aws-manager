package com.threlease.base.functions.auth;

import com.threlease.base.entites.AuthEntity;
import com.threlease.base.functions.auth.dto.LoginDto;
import com.threlease.base.utils.Hash;
import com.threlease.base.utils.responses.BasicResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    private ResponseEntity<?> login(
        @ModelAttribute @Valid LoginDto dto
    ) {
        Optional<AuthEntity> user = authService.findOneByUsername(dto.getUsername());

        BasicResponse error_response = BasicResponse.builder()
                .success(false)
                .message(Optional.of("아이디 혹은 비밀번호를 확인해주세요."))
                .data(Optional.empty())
                .build();

        if (user.isPresent()) {
            if (Objects.equals(
                    user.get().getPassword(),
                    new Hash().generateSHA512(dto.getPassword() + user.get().getSalt()))
            ) {
                BasicResponse response = BasicResponse.builder()
                        .success(true)
                        .message(Optional.empty())
                        .data(Optional.ofNullable(authService.sign(user.get().getUuid())))
                        .build();

                return ResponseEntity.status(201).body(response);
            } else {
                return ResponseEntity.status(401).body(error_response);
            }
        } else {
            return ResponseEntity.status(404).body(error_response);
        }
    }

    @GetMapping("/@status")
    private ResponseEntity<?> tokenStatus(
            @RequestHeader("Authorization") String token
    ) {
        if (!authService.validateToken(token)) {
            BasicResponse response = BasicResponse.builder()
                    .success(true)
                    .message(Optional.of("세션이 만료되었거나 알 수 없습니다."))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(401).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.empty())
                .build();

        return ResponseEntity.status(200).body(response);
    }
}
