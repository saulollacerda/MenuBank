package com.MenuBank.MenuBank.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);

    Optional<User> findByEmail(String email);
}

