package com.dataagent.platform.modules.auth.repository;

import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryAuthRepository implements AuthRepository {

    private final Map<String, AuthUser> usersByUsername = Map.of(
            "analyst01", new AuthUser(
                    "user-001",
                    "analyst01",
                    "password123",
                    "分析师一号",
                    "https://static.local/avatar/analyst01.png",
                    "analyst01@example.com",
                    "13800000001",
                    (short) 0,
                    "ACTIVE",
                    "tenant-demo",
                    Set.of("ANALYST"),
                    Set.of("dataset-sales", "dataset-finance"),
                    Set.of("phone", "id_card")
            ),
            "admin01", new AuthUser(
                    "user-002",
                    "admin01",
                    "password123",
                    "管理员一号",
                    "https://static.local/avatar/admin01.png",
                    "admin01@example.com",
                    "13800000002",
                    (short) 0,
                    "ACTIVE",
                    "tenant-demo",
                    Set.of("ADMIN"),
                    Set.of("dataset-sales", "dataset-finance", "dataset-ops"),
                    Set.of("phone", "id_card", "bank_account")
            )
    );

    private final Map<String, AuthUser> usersByUserId = usersByUsername.values().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(AuthUser::userId, user -> user));

    @Override
    public Optional<AuthUser> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        return usersByUsername.values().stream()
                .filter(user -> identifier.equalsIgnoreCase(user.username())
                        || identifier.equalsIgnoreCase(user.email())
                        || identifier.equalsIgnoreCase(user.phone()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return usersByUsername.values().stream()
                .filter(user -> email.equalsIgnoreCase(user.email()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }

        return usersByUsername.values().stream()
                .filter(user -> phone.equalsIgnoreCase(user.phone()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByUserId(String userId) {
        return Optional.ofNullable(usersByUserId.get(userId));
    }

    @Override
    public AuthUser create(AuthRegisterDTO request, String passwordHash) {
        throw new UnsupportedOperationException("in-memory auth repository does not support registration");
    }

    @Override
    public void updateLoginSuccess(String userId, LocalDateTime loginAt, String loginIp) {
        // Demo-only repository does not persist login metadata.
    }
}
