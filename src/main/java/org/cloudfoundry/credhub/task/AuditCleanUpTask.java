package org.cloudfoundry.credhub.task;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudfoundry.credhub.data.AuditCleanUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class AuditCleanUpTask {
  public static final String CRON_SCHEDULE = "0 0 0 * * *";
  private AuditCleanUpService service;
  private final Logger logger =  LogManager.getLogger();

  @Value("${audit_logs.days_retained:30}")
  private String daysRetained;

  @Autowired
  public AuditCleanUpTask(AuditCleanUpService service){
    this.service = service;
  }

  @Scheduled(cron = CRON_SCHEDULE)
  public void cleanUp(){
    logger.info("Cleaning up logs");
    service.cleanUp(Integer.valueOf(daysRetained));
  }
}
