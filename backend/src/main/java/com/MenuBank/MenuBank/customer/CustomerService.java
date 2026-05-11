package com.MenuBank.MenuBank.customer;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public List<CustomerResponse> findAll() {
        UUID ownerId = userContext.getUserId();
        return customerRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
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
