package com.MenuBank.MenuBank.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByOwnerIdAndTypeAndReferenceDataAndStatusNot(
            UUID ownerId, NotificationType type, String referenceData, NotificationStatus status);

    List<Notification> findAllByOwnerIdAndTypeAndReferenceDataAndStatusNot(
            UUID ownerId, NotificationType type, String referenceData, NotificationStatus status);

    Page<Notification> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    long countByOwnerIdAndStatus(UUID ownerId, NotificationStatus status);

    Optional<Notification> findByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
