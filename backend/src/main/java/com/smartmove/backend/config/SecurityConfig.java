
package com.smartmove.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    // ==========================================
    // PASSWORD ENCODING
    // ==========================================

    // Used by AuthService to hash passwords
    // during registration and password changes,
    // and to verify passwords during login.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
