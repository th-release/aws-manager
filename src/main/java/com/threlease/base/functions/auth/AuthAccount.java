package com.threlease.base.functions.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthAccount {
    @Value("${env.id}")
    public String id;

    @Value("${env.password}")
    public String password;

    @Value("${env.secret}")
    public String secret;
}
