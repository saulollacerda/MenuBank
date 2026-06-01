package com.MenuBank.MenuBank.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findBySubscriptionId(UUID subscriptionId);

    List<Invoice> findBySubscriptionIdAndStatus(UUID subscriptionId, InvoiceStatus status);
}
