package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.user.*;
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

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(UserRepository userRepository,
                       UserService userService,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveUserException();
        }

        String token = generateToken(user.getId(), user.getEmail(), user.getRestaurantName());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .restaurantName(user.getRestaurantName())
                .build();
    }

    public LoginResponse register(UserRequest request) {
        UserResponse userResponse = userService.create(request);

        String token = generateToken(
                userResponse.getId(),
                userResponse.getEmail(),
                userResponse.getRestaurantName()
        );

        return LoginResponse.builder()
                .token(token)
                .userId(userResponse.getId())
                .email(userResponse.getEmail())
                .restaurantName(userResponse.getRestaurantName())
                .build();
    }

    private String generateToken(UUID userId, String email, String restaurantName) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("menubank")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_EXPIRATION_HOURS, ChronoUnit.HOURS))
                .subject(userId.toString())
                .claim("email", email)
                .claim("restaurantName", restaurantName)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
