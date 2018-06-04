package io.pivotal.security.repository;

import io.pivotal.security.entity.RequestAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface RequestAuditRecordRepository extends JpaRepository<RequestAuditRecord, Long> {
  @Transactional
  void deleteByNowBefore(Instant expiryDate);
}
