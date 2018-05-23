package io.pivotal.security.task;

import org.junit.Test;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class AuditCleanUpTaskTest {
  @Test
  public void testCleanUpSchedule(){
    CronTrigger trigger = new CronTrigger(AuditCleanUpTask.CRON_SCHEDULE);

    Calendar currentTime = Calendar.getInstance();
    currentTime.set(2018, 0, 0, 21, 0, 0);
    Date firstExecutionTime = trigger.nextExecutionTime(
        new TriggerContext() {

          @Override
          public Date lastScheduledExecutionTime() {
            return currentTime.getTime();
          }

          @Override
          public Date lastActualExecutionTime() {
            return currentTime.getTime();
          }

          @Override
          public Date lastCompletionTime() {
            return currentTime.getTime();
          }
        });

    Calendar expectedTime = Calendar.getInstance();
    expectedTime.set(2018, 0, 1, 0, 0, 0);
    long timeDifference = Math.abs(firstExecutionTime.getTime() - expectedTime.getTimeInMillis());

    assertThat(timeDifference, is(lessThanOrEqualTo(2000L)));

    Date secondExecutionTime = trigger.nextExecutionTime(
        new TriggerContext() {

          @Override
          public Date lastScheduledExecutionTime() {
            return firstExecutionTime;
          }

          @Override
          public Date lastActualExecutionTime() {
            return firstExecutionTime;
          }

          @Override
          public Date lastCompletionTime() {
            return firstExecutionTime;
          }
        });

    expectedTime.set(2018, 0, 2, 0, 0, 0);
    timeDifference = Math.abs(secondExecutionTime.getTime() - expectedTime.getTimeInMillis());

    assertThat(timeDifference, is(lessThanOrEqualTo(2000L)));
  }
}
