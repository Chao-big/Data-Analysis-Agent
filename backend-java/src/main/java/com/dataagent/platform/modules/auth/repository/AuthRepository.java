package com.dataagent.platform.modules.auth.repository;

import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthRepository {

    Optional<AuthUser> findByIdentifier(String identifier);

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByPhone(String phone);

    Optional<AuthUser> findByUserId(String userId);

    AuthUser create(AuthRegisterDTO request, String passwordHash);

    void updateLoginSuccess(String userId, LocalDateTime loginAt, String loginIp);
}
