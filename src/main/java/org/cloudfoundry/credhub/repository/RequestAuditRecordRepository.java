package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.RequestAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RequestAuditRecordRepository extends JpaRepository<RequestAuditRecord, Long> {
  void deleteByNowBefore(Instant expiryDate);
}
