package ru.telproject.configuration;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.telproject.scheduler.ScheduledTask;

@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail jobDetail(){
        return JobBuilder.newJob(ScheduledTask.class)
                .withIdentity("scheduledTaskJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger(JobDetail jobDetail){
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("scheduledTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/2 * * * ?"))
                .build();
    }
}
