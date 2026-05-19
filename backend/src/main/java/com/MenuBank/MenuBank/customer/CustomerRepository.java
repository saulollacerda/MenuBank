package com.MenuBank.MenuBank.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	Optional<Customer> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Customer> findAllByOwnerId(UUID ownerId);

	Page<Customer> findAllByOwnerIdAndNameContainingIgnoreCase(UUID ownerId, String name, Pageable pageable);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

	void deleteByIdAndOwnerId(UUID id, UUID ownerId);

	Optional<Customer> findByPhoneAndOwnerId(String phone, UUID ownerId);
}
