package com.MenuBank.MenuBank.notification;

import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MerchantRepository merchantRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               MerchantRepository merchantRepository) {
        this.notificationRepository = notificationRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * Creates a notification flagging that an ingredient referenced by an imported order
     * is not registered yet. Deduplicates by (merchantId, type, canonicalName): if a pending
     * (non-RESOLVED) notification already exists for the same canonical name, returns it
     * unchanged.
     */
    @Transactional
    public Notification createMissingIngredient(String rawName, String canonicalName, UUID merchantId) {
        Optional<Notification> existing = notificationRepository
                .findByMerchantIdAndTypeAndReferenceDataAndStatusNot(
                        merchantId, NotificationType.MISSING_INGREDIENT, canonicalName, NotificationStatus.RESOLVED);
        if (existing.isPresent()) {
            return existing.get();
        }

        Notification created = Notification.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .type(NotificationType.MISSING_INGREDIENT)
                .title("Ingrediente não cadastrado")
                .message("O ingrediente '" + rawName + "' apareceu em um pedido mas não está cadastrado no sistema.")
                .referenceData(canonicalName)
                .referenceDisplay(rawName)
                .status(NotificationStatus.UNREAD)
                .createdAt(Instant.now())
                .build();
        return notificationRepository.save(created);
    }

    /**
     * Deletes every {@link NotificationType#MISSING_INGREDIENT} notification for the given
     * canonical name. Invoked when the merchant finally registers the ingredient, so the
     * alert disappears entirely instead of lingering as a resolved item.
     *
     * @return the number of notifications removed
     */
    @Transactional
    public int deleteMissingIngredient(String canonicalName, UUID merchantId) {
        return (int) notificationRepository.deleteByMerchantIdAndTypeAndReferenceData(
                merchantId, NotificationType.MISSING_INGREDIENT, canonicalName);
    }

    public Page<NotificationResponse> findAll(UUID merchantId, Pageable pageable) {
        return notificationRepository
                .findAllByMerchantIdAndStatusNotOrderByCreatedAtDesc(merchantId, NotificationStatus.RESOLVED, pageable)
                .map(this::toResponse);
    }

    public long unreadCount(UUID merchantId) {
        return notificationRepository.countByMerchantIdAndStatus(merchantId, NotificationStatus.UNREAD);
    }

    @Transactional
    public void markRead(UUID merchantId, UUID id) {
        Notification notification = notificationRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void dismiss(UUID merchantId, UUID id) {
        notificationRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notificationRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceData(n.getReferenceData())
                .referenceDisplay(n.getReferenceDisplay())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .resolvedAt(n.getResolvedAt())
                .build();
    }
}
