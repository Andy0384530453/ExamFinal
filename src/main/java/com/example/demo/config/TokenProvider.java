package com.example.demo.config;

import com.example.demo.entity.JUser;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenProvider {

  private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

  private final SecretKey secretKey;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final long expirationHours;

  public TokenProvider(
      @Value("${app.security.jwt-secret}") String secret,
      @Value("${app.security.jwt-expiration-hours:12}") long expirationHours) {
    this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(ALGORITHM).build();
    this.expirationHours = expirationHours;
  }

  public String generateToken(JUser user) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofHours(expirationHours)))
            .build();
    JwsHeader header = JwsHeader.with(ALGORITHM).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public Jwt validateToken(String token) {
    return jwtDecoder.decode(token);
  }

  public String getUserId(Jwt jwt) {
    return jwt.getSubject();
  }

  public String getRole(Jwt jwt) {
    return jwt.getClaimAsString("role");
  }

  public JwtDecoder jwtDecoder() {
    return jwtDecoder;
  }
}
