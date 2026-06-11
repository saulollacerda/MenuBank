package com.MenuBank.MenuBank.merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);

    Optional<Merchant> findByEmail(String email);

    List<Merchant> findAllByAnotaAiApiKeyIsNotNull();
}
