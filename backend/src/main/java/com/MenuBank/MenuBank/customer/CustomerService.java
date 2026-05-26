package com.MenuBank.MenuBank.customer;

import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantContext merchantContext;

    public CustomerService(CustomerRepository customerRepository,
                           MerchantRepository merchantRepository,
                           MerchantContext merchantContext) {
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.merchantContext = merchantContext;
    }

    public CustomerResponse create(CustomerRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        Customer customer = Customer.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    public CustomerResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Customer customer = customerRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return toResponse(customer);
    }

    public Page<CustomerResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return customerRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable)
                .map(this::toResponse);
    }

    public CustomerResponse update(UUID id, CustomerRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Customer customer = customerRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!customerRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteByIdAndMerchantId(id, merchantId);
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
