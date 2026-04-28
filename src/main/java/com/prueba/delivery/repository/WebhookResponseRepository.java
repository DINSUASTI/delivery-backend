package com.prueba.delivery.repository;

import com.prueba.delivery.entity.WebhookResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WebhookResponseRepository extends JpaRepository<WebhookResponse, Long> {
    Optional<WebhookResponse> findByIdempotencyKey(String idempotencyKey);
}
