package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.EventAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface EventAuditRecordRepository extends JpaRepository<EventAuditRecord, Long> {
  @Transactional
  @Modifying
  @Query(value = "delete from event_audit_record where now < ?1"
      , nativeQuery = true)
  void deleteByDateBefore(Long expiryDate);
}
