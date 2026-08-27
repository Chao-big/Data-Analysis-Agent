package com.dataagent.platform.modules.auth.repository;

import com.dataagent.platform.modules.auth.domain.model.AuthUser;

import java.util.Optional;

public interface AuthRepository {

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByUserId(String userId);
}
