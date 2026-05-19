package com.MenuBank.MenuBank.payment;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserContext userContext;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository, UserContext userContext) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.userContext = userContext;
    }

    public PaymentMethodResponse create(PaymentMethodRequest request) {
        UUID ownerId = userContext.getUserId();

        if (paymentMethodRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicatePaymentMethodException("nome");
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .feeRate(request.getFeeRate())
                .build();

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return toResponse(saved);
    }

    public PaymentMethodResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(id));
        return toResponse(paymentMethod);
    }

    public Page<PaymentMethodResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return paymentMethodRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    public PaymentMethodResponse update(UUID id, PaymentMethodRequest request) {
        UUID ownerId = userContext.getUserId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(id));

        paymentMethod.setName(request.getName());
        paymentMethod.setFeeRate(request.getFeeRate());

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!paymentMethodRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new PaymentMethodNotFoundException(id);
        }
        paymentMethodRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .feeRate(paymentMethod.getFeeRate())
                .build();
    }
}
