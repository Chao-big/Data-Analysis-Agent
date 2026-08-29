package com.dataagent.platform.modules.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, AuthSessionProperties.class})
public class JwtConfig {

    private static final byte[] RSA_ALGORITHM_IDENTIFIER = new byte[]{
            0x30, 0x0D,
            0x06, 0x09,
            0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00
    };

    private final JwtProperties jwtProperties;

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAKey rsaKey = new RSAKey.Builder(loadPublicKey())
                .privateKey(loadPrivateKey())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer()));
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private RSAPrivateKey loadPrivateKey() {
        try {
            String pem = readPem(jwtProperties.getPrivateKeyPath());
            byte[] decoded = Base64.getDecoder().decode(stripPemHeaders(pem));
            byte[] keyBytes = pem.contains("BEGIN RSA PRIVATE KEY") ? wrapPkcs1PrivateKey(decoded) : decoded;
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load JWT private key", exception);
        }
    }

    private RSAPublicKey loadPublicKey() {
        try {
            String pem = readPem(jwtProperties.getPublicKeyPath());
            byte[] decoded = Base64.getDecoder().decode(stripPemHeaders(pem));
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load JWT public key", exception);
        }
    }

    private String readPem(org.springframework.core.io.Resource resource) throws IOException {
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String stripPemHeaders(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private byte[] wrapPkcs1PrivateKey(byte[] pkcs1PrivateKey) throws IOException {
        byte[] version = new byte[]{0x02, 0x01, 0x00};
        byte[] privateKey = encodeDer((byte) 0x04, pkcs1PrivateKey);
        return encodeSequence(version, RSA_ALGORITHM_IDENTIFIER, privateKey);
    }

    private byte[] encodeSequence(byte[]... parts) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            content.write(part);
        }
        return encodeDer((byte) 0x30, content.toByteArray());
    }

    private byte[] encodeDer(byte tag, byte[] value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(tag);
        output.write(encodeLength(value.length));
        output.write(value);
        return output.toByteArray();
    }

    private byte[] encodeLength(int length) {
        if (length < 0x80) {
            return new byte[]{(byte) length};
        }

        int remaining = length;
        byte[] buffer = new byte[4];
        int index = buffer.length;
        while (remaining > 0) {
            buffer[--index] = (byte) (remaining & 0xFF);
            remaining >>= 8;
        }

        int size = buffer.length - index;
        byte[] encoded = new byte[size + 1];
        encoded[0] = (byte) (0x80 | size);
        System.arraycopy(buffer, index, encoded, 1, size);
        return encoded;
    }
}
