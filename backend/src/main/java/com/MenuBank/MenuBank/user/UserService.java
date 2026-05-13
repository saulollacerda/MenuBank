package com.MenuBank.MenuBank.user;

import com.MenuBank.MenuBank.common.ForbiddenException;
import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserContext userContext;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserContext userContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userContext = userContext;
    }

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("email");
        }
        if (userRepository.existsByCnpj(request.getCnpj())) {
            throw new DuplicateUserException("CNPJ");
        }

        User user = User.builder()
                .restaurantName(request.getRestaurantName())
                .cnpj(request.getCnpj())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse findById(UUID id) {
        ensureOwner(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    public UserResponse update(UUID id, UserRequest request) {
        ensureOwner(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setRestaurantName(request.getRestaurantName());
        user.setCnpj(request.getCnpj());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ensureOwner(id);
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private void ensureOwner(UUID id) {
        if (!userContext.getUserId().equals(id)) {
            throw new ForbiddenException("Acesso negado");
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .restaurantName(user.getRestaurantName())
                .cnpj(user.getCnpj())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
