package com.dataagent.platform.modules.auth.repository;

import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
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
                    "UNKNOWN",
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
                    "UNKNOWN",
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
    public Optional<AuthUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public Optional<AuthUser> findByUserId(String userId) {
        return Optional.ofNullable(usersByUserId.get(userId));
    }
}
