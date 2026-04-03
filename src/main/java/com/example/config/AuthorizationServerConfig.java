package com.example.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Value("${jwt.key-path:keys/jwt}")
    private String keyPath;

    @Bean
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> {
                        // Enable OpenID Connect 1.0 with default configuration
                });

        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        request -> request.getRequestURI().startsWith("/oauth2/")
                )
        );

        return http.build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = loadOrGenerateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                // Add custom claims to JWT
                context.getClaims().claim("iss_custom", "OAuth2 Authorization Server");
                context.getClaims().claim("server_version", "1.0.0");

                // Add user authorities/roles to JWT if available
                if (context.getPrincipal() != null && context.getPrincipal().getAuthorities() != null) {
                    context.getClaims().claim("authorities",
                        context.getPrincipal().getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .toList());
                }

                // Add client information
                if (context.getRegisteredClient() != null) {
                    context.getClaims().claim("client_name", context.getRegisteredClient().getClientId());
                }
            }
        };
    }

    private KeyPair loadOrGenerateRsaKey() {
        File privateKeyFile = new File(keyPath + ".private.pem");
        File publicKeyFile = new File(keyPath + ".public.pem");

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            try {
                return loadRsaKey(privateKeyFile, publicKeyFile);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to load RSA key pair from " + keyPath, ex);
            }
        }

        KeyPair keyPair = generateRsaKey();
        try {
            saveRsaKey(keyPair, privateKeyFile, publicKeyFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save RSA key pair to " + keyPath, ex);
        }
        return keyPair;
    }

    private KeyPair loadRsaKey(File privateKeyFile, File publicKeyFile) throws Exception {
        String privateKeyPem = readFile(privateKeyFile)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        String publicKeyPem = readFile(publicKeyFile)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPem)));
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPem)));

        return new KeyPair(publicKey, privateKey);
    }

    private void saveRsaKey(KeyPair keyPair, File privateKeyFile, File publicKeyFile) throws IOException {
        privateKeyFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(privateKeyFile)) {
            writer.write("-----BEGIN PRIVATE KEY-----\n");
            writer.write(Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded()));
            writer.write("\n-----END PRIVATE KEY-----\n");
        }
        try (FileWriter writer = new FileWriter(publicKeyFile)) {
            writer.write("-----BEGIN PUBLIC KEY-----\n");
            writer.write(Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPublic().getEncoded()));
            writer.write("\n-----END PUBLIC KEY-----\n");
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            char[] buf = new char[1024];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }

    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
