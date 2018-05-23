package io.pivotal.security.data;

import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.entity.AuthFailureAuditRecord;
import io.pivotal.security.entity.EventAuditRecord;
import io.pivotal.security.entity.RequestAuditRecord;
import io.pivotal.security.repository.AuthFailureAuditRecordRepository;
import io.pivotal.security.repository.EventAuditRecordRepository;
import io.pivotal.security.repository.RequestAuditRecordRepository;
import io.pivotal.security.task.AuditCleanUpTask;
import io.pivotal.security.util.CurrentTimeProvider;
import io.pivotal.security.util.DatabaseProfileResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.pivotal.security.helper.TestHelper.mockOutCurrentTimeProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@Transactional
public class AuditCleanUpServiceTest {
  @Autowired
  private AuditCleanUpService subject;

  @Autowired
  private AuthFailureAuditRecordRepository authFailureAuditRepo;

  @Autowired
  private EventAuditRecordRepository eventAuditRepo;

  @Autowired
  private RequestAuditRecordRepository requestAuditRepo;

  @Autowired
  private AuditCleanUpTask auditCleanUpTask;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @MockBean
  CurrentTimeProvider currentTimeProvider;

  private AuthFailureAuditRecord authFailureAuditRecordOld;
  private AuthFailureAuditRecord authFailureAuditRecordNew;
  private EventAuditRecord eventAuditRecordOld;
  private EventAuditRecord eventAuditRecordNew;
  private RequestAuditRecord requestAuditRecordOld;
  private RequestAuditRecord requestAuditRecordNew;

  private static final int DAYS_RETAINED = 15;
  Instant now = Instant.now();
  Instant frozenTime = now.minus(DAYS_RETAINED + 1, ChronoUnit.DAYS);

  @Before
  public void setUp(){
    authFailureAuditRecordOld = new AuthFailureAuditRecord();
    authFailureAuditRecordOld.setAuthMethod("uaa");
    authFailureAuditRecordNew = new AuthFailureAuditRecord();
    authFailureAuditRecordNew.setAuthMethod("uaa");

    mockOutCurrentTimeProvider(currentTimeProvider).accept(now.toEpochMilli());
    authFailureAuditRepo.save(authFailureAuditRecordNew);

    mockOutCurrentTimeProvider(currentTimeProvider).accept(frozenTime.toEpochMilli());
    authFailureAuditRepo.save(authFailureAuditRecordOld);

    assertThat(authFailureAuditRepo.count(), equalTo(2L));

    eventAuditRecordOld = new EventAuditRecord("", "", "", UUID.randomUUID(), false, "", "");
    eventAuditRecordNew = new EventAuditRecord("", "", "", UUID.randomUUID(), false, "", "");

    mockOutCurrentTimeProvider(currentTimeProvider).accept(now.toEpochMilli());
    eventAuditRepo.save(eventAuditRecordNew);

    mockOutCurrentTimeProvider(currentTimeProvider).accept(frozenTime.toEpochMilli());
    eventAuditRepo.save(eventAuditRecordOld);

    assertThat(eventAuditRepo.count(), equalTo(2L));

    requestAuditRecordOld = new RequestAuditRecord(UUID.randomUUID(), frozenTime, "uaa", "", "", "", 0L, 0, "", "", "", "", 0, "", "", "", "", "");
    requestAuditRecordNew = new RequestAuditRecord(UUID.randomUUID(), now, "uaa", "", "", "", 0L, 0, "", "", "", "", 0, "", "", "", "", "");

    mockOutCurrentTimeProvider(currentTimeProvider).accept(now.toEpochMilli());
    requestAuditRepo.save(requestAuditRecordNew);

    mockOutCurrentTimeProvider(currentTimeProvider).accept(frozenTime.toEpochMilli());
    requestAuditRepo.save(requestAuditRecordOld);

    assertThat(requestAuditRepo.count(), equalTo(2L));
  }

  @Test
  public void cleanUpShouldRemoveOldRecords(){
    subject.cleanUp(DAYS_RETAINED);

    assertThat(authFailureAuditRepo.count(), equalTo(1L));
    assertThat(authFailureAuditRepo.findAll().get(0).getId(), equalTo(authFailureAuditRecordNew.getId()));
    assertThat(eventAuditRepo.count(), equalTo(1L));
    assertThat(eventAuditRepo.findAll().get(0).getUuid(), equalTo(eventAuditRecordNew.getUuid()));
    assertThat(requestAuditRepo.count(), equalTo(1L));
    assertThat(requestAuditRepo.findAll().get(0).getUuid(), equalTo(requestAuditRecordNew.getUuid()));
  }
}
