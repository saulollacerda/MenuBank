package com.MenuBank.MenuBank.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findBySubscriptionId(UUID subscriptionId);

    Optional<Invoice> findByAbacatepayBillingId(String abacatepayBillingId);

    List<Invoice> findBySubscriptionIdAndStatus(UUID subscriptionId, InvoiceStatus status);
}
