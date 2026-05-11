package com.MenuBank.MenuBank.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	Optional<Customer> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Customer> findAllByOwnerId(UUID ownerId);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

	void deleteByIdAndOwnerId(UUID id, UUID ownerId);
}

