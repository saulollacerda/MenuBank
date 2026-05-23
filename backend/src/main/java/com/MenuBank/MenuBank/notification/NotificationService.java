package com.MenuBank.MenuBank.notification;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserContext userContext;

    public NotificationService(NotificationRepository notificationRepository, UserContext userContext) {
        this.notificationRepository = notificationRepository;
        this.userContext = userContext;
    }

    /**
     * Creates a notification flagging that an ingredient referenced by an imported order
     * is not registered yet. Deduplicates by (ownerId, type, canonicalName): if a pending
     * (non-RESOLVED) notification already exists for the same canonical name, returns it
     * unchanged.
     */
    @Transactional
    public Notification createMissingIngredient(String rawName, String canonicalName, UUID ownerId) {
        Optional<Notification> existing = notificationRepository
                .findByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                        ownerId, NotificationType.MISSING_INGREDIENT, canonicalName, NotificationStatus.RESOLVED);
        if (existing.isPresent()) {
            return existing.get();
        }

        Notification created = Notification.builder()
                .ownerId(ownerId)
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
     * Marks every pending {@link NotificationType#MISSING_INGREDIENT} notification for
     * the given canonical name as RESOLVED. Invoked when the user finally registers the
     * ingredient.
     */
    @Transactional
    public int resolveMissingIngredient(String canonicalName, UUID ownerId) {
        List<Notification> pending = notificationRepository
                .findAllByOwnerIdAndTypeAndReferenceDataAndStatusNot(
                        ownerId, NotificationType.MISSING_INGREDIENT, canonicalName, NotificationStatus.RESOLVED);
        if (pending.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();
        pending.forEach(n -> {
            n.setStatus(NotificationStatus.RESOLVED);
            n.setResolvedAt(now);
        });
        notificationRepository.saveAll(pending);
        return pending.size();
    }

    public Page<NotificationResponse> findAll(Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        return notificationRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(this::toResponse);
    }

    public long unreadCount() {
        UUID ownerId = userContext.getUserId();
        return notificationRepository.countByOwnerIdAndStatus(ownerId, NotificationStatus.UNREAD);
    }

    @Transactional
    public void markRead(UUID id) {
        UUID ownerId = userContext.getUserId();
        Notification notification = notificationRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void dismiss(UUID id) {
        UUID ownerId = userContext.getUserId();
        notificationRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notificationRepository.deleteByIdAndOwnerId(id, ownerId);
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
