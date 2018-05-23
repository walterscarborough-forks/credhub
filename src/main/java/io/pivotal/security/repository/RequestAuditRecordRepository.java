package io.pivotal.security.repository;

import io.pivotal.security.entity.RequestAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RequestAuditRecordRepository extends JpaRepository<RequestAuditRecord, Long> {
  void deleteByNowBefore(Instant expiryDate);
}
