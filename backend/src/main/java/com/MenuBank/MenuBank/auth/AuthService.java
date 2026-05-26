package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.merchant.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final MerchantRepository merchantRepository;
    private final MerchantService merchantService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(MerchantRepository merchantRepository,
                       MerchantService merchantService,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder) {
        this.merchantRepository = merchantRepository;
        this.merchantService = merchantService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Merchant merchant = merchantRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), merchant.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new InactiveMerchantException();
        }

        String token = generateToken(merchant.getId(), merchant.getEmail(), merchant.getMerchantName());

        return LoginResponse.builder()
                .token(token)
                .merchantId(merchant.getId())
                .email(merchant.getEmail())
                .merchantName(merchant.getMerchantName())
                .build();
    }

    public LoginResponse register(MerchantRequest request) {
        MerchantResponse merchantResponse = merchantService.create(request);

        String token = generateToken(
                merchantResponse.getId(),
                merchantResponse.getEmail(),
                merchantResponse.getMerchantName()
        );

        return LoginResponse.builder()
                .token(token)
                .merchantId(merchantResponse.getId())
                .email(merchantResponse.getEmail())
                .merchantName(merchantResponse.getMerchantName())
                .build();
    }

    private String generateToken(UUID merchantId, String email, String merchantName) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("menubank")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS))
                .subject(merchantId.toString())
                .claim("email", email)
                .claim("merchantName", merchantName)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
