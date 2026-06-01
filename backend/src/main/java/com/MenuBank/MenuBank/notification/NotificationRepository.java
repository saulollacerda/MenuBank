package com.MenuBank.MenuBank.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByMerchantIdAndTypeAndReferenceDataAndStatusNot(
            UUID merchantId, NotificationType type, String referenceData, NotificationStatus status);

    List<Notification> findAllByMerchantIdAndTypeAndReferenceDataAndStatusNot(
            UUID merchantId, NotificationType type, String referenceData, NotificationStatus status);

    Page<Notification> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    Page<Notification> findAllByMerchantIdAndStatusNotOrderByCreatedAtDesc(
            UUID merchantId, NotificationStatus status, Pageable pageable);

    long countByMerchantIdAndStatus(UUID merchantId, NotificationStatus status);

    Optional<Notification> findByIdAndMerchantId(UUID id, UUID merchantId);

    void deleteByIdAndMerchantId(UUID id, UUID merchantId);
}
