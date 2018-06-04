package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.AuthFailureAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface AuthFailureAuditRecordRepository extends JpaRepository<AuthFailureAuditRecord, Long> {
  @Transactional
  void deleteByNowBefore(Instant expiryDate);
}
