package com.threlease.base.functions.scheduler;

import com.threlease.base.entites.AuthEntity;
import com.threlease.base.functions.auth.AuthAccount;
import com.threlease.base.functions.auth.AuthService;
import com.threlease.base.functions.auth.Roles;
import com.threlease.base.utils.GetRandom;
import com.threlease.base.utils.Hash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfiguration {
    private final AuthService authService;
    private final AuthAccount authAccount;

    @Scheduled(cron = "*/1 * * * * *")
    public void sec1() {
        generatedRoot();
    }

    private void generatedRoot() {
        Optional<AuthEntity> root = authService.findOneByUsername(authAccount.id);
        if (root.isEmpty()) {
            String salt = new GetRandom().run("all", 32);

            AuthEntity user = AuthEntity.builder()
                    .username(authAccount.id)
                    .password(new Hash().generateSHA512(authAccount.password + salt))
                    .salt(salt)
                    .role(Roles.ROLE_ROOT)
                    .build();

            authService.authSave(user);
        }
    }
}
