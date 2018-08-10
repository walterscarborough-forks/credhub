package io.pivotal.security.repository;

import io.pivotal.security.entity.AuthFailureAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface AuthFailureAuditRecordRepository extends
    JpaRepository<AuthFailureAuditRecord, Long> {

  @Transactional
  @Modifying
  @Query(value = "delete from auth_failure_audit_record where now < ?1"
      , nativeQuery = true)
  void deleteByDateBefore(Long expiryDate);
}
