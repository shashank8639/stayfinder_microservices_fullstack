package com.stayfinder.auth.service;

import com.stayfinder.auth.entity.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;
    private final String keyId;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${stayfinder.jwt.issuer}") String issuer,
            @Value("${stayfinder.jwt.ttl}") Duration ttl,
            com.nimbusds.jose.jwk.RSAKey rsaKey
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttl = ttl;
        this.keyId = rsaKey.getKeyID();
    }

    public String issueToken(UserAccount user) {
        Instant issuedAt = Instant.now();
        List<String> roles = user.getRoles().stream().map(Enum::name).sorted().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
