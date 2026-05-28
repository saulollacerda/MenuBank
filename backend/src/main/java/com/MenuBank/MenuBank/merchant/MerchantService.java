package com.MenuBank.MenuBank.merchant;

import com.MenuBank.MenuBank.common.ForbiddenException;
import com.MenuBank.MenuBank.common.MerchantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantContext merchantContext;

    public MerchantService(MerchantRepository merchantRepository, PasswordEncoder passwordEncoder, MerchantContext merchantContext) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.merchantContext = merchantContext;
    }

    public MerchantResponse create(MerchantRequest request) {
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateMerchantException("email");
        }
        if (merchantRepository.existsByCnpj(request.getCnpj())) {
            throw new DuplicateMerchantException("CNPJ");
        }

        Merchant merchant = Merchant.builder()
                .merchantName(request.getMerchantName())
                .cnpj(request.getCnpj())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .status(MerchantStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    public MerchantResponse findById(UUID id) {
        ensureOwner(id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));
        return toResponse(merchant);
    }

    public MerchantResponse update(UUID id, MerchantRequest request) {
        ensureOwner(id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id));

        merchant.setMerchantName(request.getMerchantName());
        merchant.setCnpj(request.getCnpj());
        merchant.setEmail(request.getEmail());
        merchant.setPassword(passwordEncoder.encode(request.getPassword()));
        merchant.setPhone(request.getPhone());
        merchant.setAnotaAiApiKey(request.getAnotaAiApiKey());

        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    public MerchantResponse updateAnotaAIKey(AnotaAIKeyRequest request) {
        UUID currentMerchantId = merchantContext.getMerchantId();
        Merchant merchant = merchantRepository.findById(currentMerchantId)
                .orElseThrow(() -> new MerchantNotFoundException(currentMerchantId));

        merchant.setAnotaAiApiKey(request.getAnotaAiApiKey());
        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    public MerchantResponse findMe() {
        UUID currentMerchantId = merchantContext.getMerchantId();
        Merchant merchant = merchantRepository.findById(currentMerchantId)
                .orElseThrow(() -> new MerchantNotFoundException(currentMerchantId));
        return toResponse(merchant);
    }

    @Transactional
    public MerchantResponse updateMe(MerchantUpdateRequest request) {
        UUID currentMerchantId = merchantContext.getMerchantId();
        Merchant merchant = merchantRepository.findById(currentMerchantId)
                .orElseThrow(() -> new MerchantNotFoundException(currentMerchantId));

        if (request.getMerchantName() != null) merchant.setMerchantName(request.getMerchantName());
        if (request.getPhone() != null) merchant.setPhone(request.getPhone());
        if (request.getAddress() != null) merchant.setAddress(request.getAddress());
        if (request.getLogoUrl() != null) merchant.setLogoUrl(request.getLogoUrl());
        if (request.getOpeningHours() != null) merchant.setOpeningHours(request.getOpeningHours());

        Merchant saved = merchantRepository.save(merchant);
        return toResponse(saved);
    }

    public MerchantPreferences getMyPreferences() {
        UUID currentMerchantId = merchantContext.getMerchantId();
        Merchant merchant = merchantRepository.findById(currentMerchantId)
                .orElseThrow(() -> new MerchantNotFoundException(currentMerchantId));
        return merchant.getPreferences() != null
                ? merchant.getPreferences()
                : MerchantPreferences.builder().build();
    }

    @Transactional
    public MerchantPreferences updateMyPreferences(MerchantPreferences preferences) {
        UUID currentMerchantId = merchantContext.getMerchantId();
        Merchant merchant = merchantRepository.findById(currentMerchantId)
                .orElseThrow(() -> new MerchantNotFoundException(currentMerchantId));

        merchant.setPreferences(preferences);
        Merchant saved = merchantRepository.save(merchant);
        return saved.getPreferences();
    }

    @Transactional
    public void delete(UUID id) {
        ensureOwner(id);
        if (!merchantRepository.existsById(id)) {
            throw new MerchantNotFoundException(id);
        }
        merchantRepository.deleteById(id);
    }

    private void ensureOwner(UUID id) {
        if (!merchantContext.getMerchantId().equals(id)) {
            throw new ForbiddenException("Acesso negado");
        }
    }

    private MerchantResponse toResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .merchantName(merchant.getMerchantName())
                .cnpj(merchant.getCnpj())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .anotaAiApiKey(merchant.getAnotaAiApiKey())
                .address(merchant.getAddress())
                .logoUrl(merchant.getLogoUrl())
                .openingHours(merchant.getOpeningHours())
                .preferences(merchant.getPreferences())
                .build();
    }
}
