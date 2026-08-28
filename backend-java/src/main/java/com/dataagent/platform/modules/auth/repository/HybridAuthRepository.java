package com.dataagent.platform.modules.auth.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.model.AuthUser;
import com.dataagent.platform.modules.auth.domain.po.AuthUserPO;
import com.dataagent.platform.modules.auth.mapper.AuthUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class HybridAuthRepository implements AuthRepository {

    private final AuthUserMapper authUserMapper;

    private final Map<String, AuthUser> fallbackUsersByUsername = Map.of(
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

    @Override
    public Optional<AuthUser> findByIdentifier(String identifier) {
        String normalized = normalize(identifier);
        if (normalized == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserPO>()
                .eq(AuthUserPO::getUsername, normalized)
                .or()
                .eq(AuthUserPO::getEmail, normalized)
                .or()
                .eq(AuthUserPO::getPhone, normalized)
                .last("LIMIT 1"));
        if (user != null) {
            return Optional.of(toDomain(user));
        }

        return fallbackUsersByUsername.values().stream()
                .filter(candidate -> normalized.equalsIgnoreCase(candidate.username())
                        || normalized.equalsIgnoreCase(candidate.email())
                        || normalized.equalsIgnoreCase(candidate.phone()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        String normalized = normalize(username);
        if (normalized == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserPO>()
                .eq(AuthUserPO::getUsername, normalized)
                .last("LIMIT 1"));
        if (user != null) {
            return Optional.of(toDomain(user));
        }

        return Optional.ofNullable(fallbackUsersByUsername.get(normalized));
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserPO>()
                .eq(AuthUserPO::getEmail, normalized)
                .last("LIMIT 1"));
        if (user != null) {
            return Optional.of(toDomain(user));
        }

        return fallbackUsersByUsername.values().stream()
                .filter(candidate -> normalized.equalsIgnoreCase(candidate.email()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByPhone(String phone) {
        String normalized = normalize(phone);
        if (normalized == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectOne(new LambdaQueryWrapper<AuthUserPO>()
                .eq(AuthUserPO::getPhone, normalized)
                .last("LIMIT 1"));
        if (user != null) {
            return Optional.of(toDomain(user));
        }

        return fallbackUsersByUsername.values().stream()
                .filter(candidate -> normalized.equalsIgnoreCase(candidate.phone()))
                .findFirst();
    }

    @Override
    public Optional<AuthUser> findByUserId(String userId) {
        String normalized = normalize(userId);
        if (normalized == null) {
            return Optional.empty();
        }

        AuthUserPO user = authUserMapper.selectById(normalized);
        if (user != null) {
            return Optional.of(toDomain(user));
        }

        return fallbackUsersByUsername.values().stream()
                .filter(candidate -> normalized.equals(candidate.userId()))
                .findFirst();
    }

    @Override
    public AuthUser create(AuthRegisterDTO request, String passwordHash) {
        AuthUserPO user = new AuthUserPO();
        user.setUsername(normalize(request.username()));
        user.setPasswordHash(passwordHash);
        user.setNickname(normalize(request.nickname()));
        user.setAvatarUrl(normalize(request.avatarUrl()));
        user.setEmail(normalize(request.email()));
        user.setPhone(normalize(request.phone()));
        user.setGender(normalize(request.gender()));
        user.setStatus("ACTIVE");
        user.setRemark(normalize(request.remark()));
        authUserMapper.insert(user);
        return toDomain(user);
    }

    private AuthUser toDomain(AuthUserPO user) {
        return new AuthUser(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getStatus(),
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
