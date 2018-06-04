package io.pivotal.security.repository;

import io.pivotal.security.entity.AuthFailureAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.time.Instant;

public interface AuthFailureAuditRecordRepository extends
    JpaRepository<AuthFailureAuditRecord, Long> {

  @Transactional
  void deleteByNowBefore(Instant expiryDate);

}
