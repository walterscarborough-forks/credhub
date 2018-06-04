package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.EventAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface EventAuditRecordRepository extends JpaRepository<EventAuditRecord, Long> {
  @Transactional
  void deleteByNowBefore(Instant expiryDate);
}
