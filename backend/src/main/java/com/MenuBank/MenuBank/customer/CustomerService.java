package com.MenuBank.MenuBank.customer;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserContext userContext;

    public CustomerService(CustomerRepository customerRepository, UserContext userContext) {
        this.customerRepository = customerRepository;
        this.userContext = userContext;
    }

    public CustomerResponse create(CustomerRequest request) {
        UUID ownerId = userContext.getUserId();

        Customer customer = Customer.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    public CustomerResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Customer customer = customerRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return toResponse(customer);
    }

    public Page<CustomerResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return customerRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    public CustomerResponse update(UUID id, CustomerRequest request) {
        UUID ownerId = userContext.getUserId();
        Customer customer = customerRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!customerRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .build();
    }
}
