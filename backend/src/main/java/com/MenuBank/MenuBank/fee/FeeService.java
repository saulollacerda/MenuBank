package com.MenuBank.MenuBank.fee;

import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FeeService {

    private final FeeRepository feeRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantContext merchantContext;

    public FeeService(FeeRepository feeRepository,
                      MerchantRepository merchantRepository,
                      MerchantContext merchantContext) {
        this.feeRepository = feeRepository;
        this.merchantRepository = merchantRepository;
        this.merchantContext = merchantContext;
    }

    public FeeResponse create(FeeRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        if (feeRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateFeeException("nome");
        }

        Fee fee = Fee.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .name(request.getName())
                .feeRate(request.getFeeRate())
                .build();

        Fee saved = feeRepository.save(fee);
        return toResponse(saved);
    }

    public FeeResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Fee fee = feeRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new FeeNotFoundException(id));
        return toResponse(fee);
    }

    public Page<FeeResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return feeRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable)
                .map(this::toResponse);
    }

    public FeeResponse update(UUID id, FeeRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Fee fee = feeRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new FeeNotFoundException(id));

        fee.setName(request.getName());
        fee.setFeeRate(request.getFeeRate());

        Fee saved = feeRepository.save(fee);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!feeRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new FeeNotFoundException(id);
        }
        feeRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private FeeResponse toResponse(Fee fee) {
        return FeeResponse.builder()
                .id(fee.getId())
                .name(fee.getName())
                .feeRate(fee.getFeeRate())
                .build();
    }
}
