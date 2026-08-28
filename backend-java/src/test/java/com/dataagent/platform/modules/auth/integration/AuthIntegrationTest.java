package com.dataagent.platform.modules.auth.integration;

import com.dataagent.platform.DataAgentApplication;
import com.dataagent.platform.modules.auth.domain.dto.AuthRegisterDTO;
import com.dataagent.platform.modules.auth.domain.dto.RefreshTokenRequest;
import com.dataagent.platform.modules.auth.mapper.AuthUserMapper;
import com.dataagent.platform.modules.auth.repository.AuthRepository;
import com.dataagent.platform.modules.auth.service.AuthTokenStoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = DataAgentApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
        }
)
@AutoConfigureMockMvc
@Import(AuthIntegrationTest.AuthIntegrationTestConfig.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryIntegrationAuthRepository authRepository;

    @Autowired
    private InMemoryIntegrationAuthTokenStoreService authTokenStoreService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthUserMapper authUserMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        authRepository.reset(passwordEncoder);
        authTokenStoreService.clear();
    }

    @Test
    void registerRefreshLogoutFlowShouldRotateAndInvalidateTokens() throws Exception {
        AuthRegisterDTO request = new AuthRegisterDTO(
                "new-user",
                "Password@123",
                "新用户",
                "https://static.local/avatar/new-user.png",
                "new-user@example.com",
                "13800000011",
                "UNKNOWN",
                "integration test"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("new-user"))
                .andReturn();

        JsonNode registerData = bodyData(registerResult);
        String accessToken = registerData.get("accessToken").asText();
        String refreshToken = registerData.get("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(registerData.get("userId").asText()))
                .andExpect(jsonPath("$.data.nickname").value("新用户"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode refreshData = bodyData(refreshResult);
        String rotatedAccessToken = refreshData.get("accessToken").asText();
        String rotatedRefreshToken = refreshData.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(rotatedAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(rotatedAccessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void loginShouldSupportEmailIdentifier() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "analyst01@example.com",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-001"))
                .andExpect(jsonPath("$.data.roles[0]").value("ANALYST"));
    }

    @Test
    void accessContextShouldIntersectRequestedDatasets() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "analyst01",
                                  "password": "Password@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = bodyData(loginResult).get("accessToken").asText();

        mockMvc.perform(post("/api/auth/access-context")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "datasetIds": ["dataset-sales", "dataset-ops"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user-001"))
                .andExpect(jsonPath("$.data.allowedDatasets.length()").value(1))
                .andExpect(jsonPath("$.data.allowedDatasets[0]").value("dataset-sales"));
    }

    private JsonNode bodyData(MvcResult mvcResult) throws Exception {
        return objectMapper.readTree(mvcResult.getResponse().getContentAsByteArray()).get("data");
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    @TestConfiguration
    static class AuthIntegrationTestConfig {

        @Bean
        InMemoryIntegrationAuthRepository inMemoryIntegrationAuthRepository(PasswordEncoder passwordEncoder) {
            InMemoryIntegrationAuthRepository repository = new InMemoryIntegrationAuthRepository();
            repository.reset(passwordEncoder);
            return repository;
        }

        @Bean
        @Primary
        AuthRepository authRepository(InMemoryIntegrationAuthRepository repository) {
            return repository;
        }

        @Bean
        InMemoryIntegrationAuthTokenStoreService inMemoryIntegrationAuthTokenStoreService() {
            return new InMemoryIntegrationAuthTokenStoreService();
        }

        @Bean
        @Primary
        AuthTokenStoreService authTokenStoreService(InMemoryIntegrationAuthTokenStoreService service) {
            return service;
        }
    }

    static class InMemoryIntegrationAuthRepository implements AuthRepository {

        private final AtomicLong idGenerator = new AtomicLong(1000L);
        private final Map<String, IntegrationAuthUserRecord> usersById = new ConcurrentHashMap<>();

        void reset(PasswordEncoder passwordEncoder) {
            usersById.clear();
            idGenerator.set(1000L);
            save(seedUser(
                    "user-001",
                    "analyst01",
                    passwordEncoder.encode("Password@123"),
                    "分析师一号",
                    "analyst01@example.com",
                    "13800000001",
                    Set.of("ANALYST"),
                    Set.of("dataset-sales", "dataset-finance"),
                    Set.of("phone")
            ));
            save(seedUser(
                    "user-002",
                    "admin01",
                    passwordEncoder.encode("Password@123"),
                    "管理员一号",
                    "admin01@example.com",
                    "13800000002",
                    Set.of("ADMIN"),
                    Set.of("dataset-sales", "dataset-finance", "dataset-ops"),
                    Set.of("phone", "id_card")
            ));
        }

        @Override
        public Optional<com.dataagent.platform.modules.auth.domain.model.AuthUser> findByIdentifier(String identifier) {
            String normalized = normalize(identifier);
            if (normalized == null) {
                return Optional.empty();
            }

            return usersById.values().stream()
                    .filter(user -> normalized.equalsIgnoreCase(user.username())
                            || normalized.equalsIgnoreCase(user.email())
                            || normalized.equalsIgnoreCase(user.phone()))
                    .findFirst()
                    .map(IntegrationAuthUserRecord::toDomain);
        }

        @Override
        public Optional<com.dataagent.platform.modules.auth.domain.model.AuthUser> findByUsername(String username) {
            String normalized = normalize(username);
            if (normalized == null) {
                return Optional.empty();
            }

            return usersById.values().stream()
                    .filter(user -> normalized.equalsIgnoreCase(user.username()))
                    .findFirst()
                    .map(IntegrationAuthUserRecord::toDomain);
        }

        @Override
        public Optional<com.dataagent.platform.modules.auth.domain.model.AuthUser> findByEmail(String email) {
            String normalized = normalize(email);
            if (normalized == null) {
                return Optional.empty();
            }

            return usersById.values().stream()
                    .filter(user -> normalized.equalsIgnoreCase(user.email()))
                    .findFirst()
                    .map(IntegrationAuthUserRecord::toDomain);
        }

        @Override
        public Optional<com.dataagent.platform.modules.auth.domain.model.AuthUser> findByPhone(String phone) {
            String normalized = normalize(phone);
            if (normalized == null) {
                return Optional.empty();
            }

            return usersById.values().stream()
                    .filter(user -> normalized.equalsIgnoreCase(user.phone()))
                    .findFirst()
                    .map(IntegrationAuthUserRecord::toDomain);
        }

        @Override
        public Optional<com.dataagent.platform.modules.auth.domain.model.AuthUser> findByUserId(String userId) {
            return Optional.ofNullable(usersById.get(normalize(userId)))
                    .map(IntegrationAuthUserRecord::toDomain);
        }

        @Override
        public com.dataagent.platform.modules.auth.domain.model.AuthUser create(
                AuthRegisterDTO request,
                String passwordHash
        ) {
            String userId = String.valueOf(idGenerator.incrementAndGet());
            IntegrationAuthUserRecord record = new IntegrationAuthUserRecord(
                    userId,
                    normalize(request.username()),
                    passwordHash,
                    normalize(request.nickname()),
                    normalizeNullable(request.avatarUrl()),
                    normalize(request.email()),
                    normalize(request.phone()),
                    normalizeNullable(request.gender()),
                    "ACTIVE",
                    "tenant-demo",
                    Set.of("ANALYST"),
                    Set.of("dataset-sales"),
                    Set.of("phone")
            );
            save(record);
            return record.toDomain();
        }

        private IntegrationAuthUserRecord seedUser(
                String userId,
                String username,
                String passwordHash,
                String nickname,
                String email,
                String phone,
                Set<String> roles,
                Set<String> allowedDatasets,
                Set<String> maskedColumns
        ) {
            return new IntegrationAuthUserRecord(
                    userId,
                    username,
                    passwordHash,
                    nickname,
                    "https://static.local/avatar/" + username + ".png",
                    email,
                    phone,
                    "UNKNOWN",
                    "ACTIVE",
                    "tenant-demo",
                    roles,
                    allowedDatasets,
                    maskedColumns
            );
        }

        private void save(IntegrationAuthUserRecord user) {
            usersById.put(user.userId(), user);
        }

        private String normalize(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }

        private String normalizeNullable(String value) {
            return normalize(value);
        }
    }

    record IntegrationAuthUserRecord(
            String userId,
            String username,
            String passwordHash,
            String nickname,
            String avatarUrl,
            String email,
            String phone,
            String gender,
            String status,
            String tenantId,
            Set<String> roles,
            Set<String> allowedDatasets,
            Set<String> maskedColumns
    ) {

        com.dataagent.platform.modules.auth.domain.model.AuthUser toDomain() {
            return new com.dataagent.platform.modules.auth.domain.model.AuthUser(
                    userId,
                    username,
                    passwordHash,
                    nickname,
                    avatarUrl,
                    email,
                    phone,
                    gender,
                    status,
                    tenantId,
                    roles,
                    allowedDatasets,
                    maskedColumns
            );
        }
    }

    static class InMemoryIntegrationAuthTokenStoreService implements AuthTokenStoreService {

        private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
        private final Map<String, Duration> refreshTtls = new ConcurrentHashMap<>();
        private final Map<String, Duration> blacklistedAccessTokens = new ConcurrentHashMap<>();

        @Override
        public void storeRefreshToken(String userId, String refreshToken, Duration ttl) {
            if (isBlank(userId) || isBlank(refreshToken) || invalidTtl(ttl)) {
                return;
            }
            refreshTokens.put(userId, refreshToken);
            refreshTtls.put(userId, ttl);
        }

        @Override
        public boolean matchesRefreshToken(String userId, String refreshToken) {
            return refreshToken != null && refreshToken.equals(refreshTokens.get(userId));
        }

        @Override
        public void removeRefreshToken(String userId) {
            refreshTokens.remove(userId);
            refreshTtls.remove(userId);
        }

        @Override
        public void blacklistAccessToken(String userId, String tokenId, Duration ttl) {
            if (isBlank(userId) || isBlank(tokenId) || invalidTtl(ttl)) {
                return;
            }
            blacklistedAccessTokens.put(userId + ":" + tokenId, ttl);
        }

        @Override
        public boolean isAccessTokenBlacklisted(String userId, String tokenId) {
            return blacklistedAccessTokens.containsKey(userId + ":" + tokenId);
        }

        void clear() {
            refreshTokens.clear();
            refreshTtls.clear();
            blacklistedAccessTokens.clear();
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private boolean invalidTtl(Duration ttl) {
            return ttl == null || ttl.isZero() || ttl.isNegative();
        }
    }
}
