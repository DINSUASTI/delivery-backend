package com.prueba.delivery.repository;

import com.prueba.delivery.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
    List<WebhookLog> findByResponseIdOrderByIdAsc(Long responseId);
}