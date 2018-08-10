package org.cloudfoundry.credhub.repository;

import org.cloudfoundry.credhub.entity.RequestAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface RequestAuditRecordRepository extends JpaRepository<RequestAuditRecord, Long> {
  @Transactional
  @Modifying
  @Query(value = "delete from request_audit_record where now < ?1"
      , nativeQuery = true)
  void deleteByDateBefore(Long expiryDate);
}
