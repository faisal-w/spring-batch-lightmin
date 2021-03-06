package org.tuxdevelop.spring.batch.lightmin.api.controller;


import lombok.Getter;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.tuxdevelop.spring.batch.lightmin.ITConfigurationApplication;
import org.tuxdevelop.spring.batch.lightmin.admin.domain.*;
import org.tuxdevelop.spring.batch.lightmin.admin.repository.JobConfigurationRepository;
import org.tuxdevelop.spring.batch.lightmin.configuration.SpringBatchLightminConfigurationProperties;
import org.tuxdevelop.spring.batch.lightmin.exception.NoSuchJobConfigurationException;
import org.tuxdevelop.spring.batch.lightmin.service.AdminService;
import org.tuxdevelop.test.configuration.ITConfiguration;
import org.tuxdevelop.test.configuration.ITJobConfiguration;

import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ITConfigurationApplication.class, ITConfiguration.class, ITJobConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class CommonControllerIT {

    public static final String LOCALHOST = "http://localhost";

    @Autowired
    @Getter
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AdminService adminService;

    @Autowired
    private EmbeddedWebApplicationContext embeddedWebApplicationContext;

    @Autowired
    private Job simpleJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobConfigurationRepository jobConfigurationRepository;
    @Autowired
    private SpringBatchLightminConfigurationProperties springBatchLightminConfigurationProperties;

    @Autowired
    protected RestTemplate restTemplate;

    protected Long addedJobConfigurationId;
    protected Long launchedJobExecutionId;
    protected Long launchedJobInstanceId;
    protected Long launchedStepExecutionId;

    protected int getServerPort() {
        return this.embeddedWebApplicationContext.getEmbeddedServletContainer().getPort();
    }


    protected void addJobConfigurations() {
        final JobConfiguration jobConfiguration = createJobConfiguration();
        this.adminService.saveJobConfiguration(jobConfiguration);
        final Collection<JobConfiguration> jobConfigurations = this.adminService.getJobConfigurationsByJobName("simpleJob");
        for (final JobConfiguration configuration : jobConfigurations) {
            this.addedJobConfigurationId = configuration.getJobConfigurationId();
        }
    }

    protected void launchSimpleJob() {
        try {
            final JobExecution execution = this.jobLauncher.run(this.simpleJob, new JobParametersBuilder().addDate("date", new
                    Date()).toJobParameters());
            this.launchedJobExecutionId = execution.getId();
            this.launchedJobInstanceId = execution.getJobInstance().getId();
            this.launchedStepExecutionId = execution.getStepExecutions().iterator().next().getId();
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    protected JobConfiguration createJobConfiguration() {
        final JobSchedulerConfiguration jobSchedulerConfiguration = new JobSchedulerConfiguration();
        jobSchedulerConfiguration.setFixedDelay(100000L);
        jobSchedulerConfiguration.setInitialDelay(100000L);
        jobSchedulerConfiguration.setJobSchedulerType(JobSchedulerType.PERIOD);
        jobSchedulerConfiguration.setTaskExecutorType(TaskExecutorType.ASYNCHRONOUS);
        jobSchedulerConfiguration.setSchedulerStatus(SchedulerStatus.INITIALIZED);
        final JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setJobName("simpleJob");
        jobConfiguration.setJobIncrementer(JobIncrementer.DATE);
        jobConfiguration.setJobSchedulerConfiguration(jobSchedulerConfiguration);
        return jobConfiguration;
    }

    protected void cleanUp() {
        final Collection<JobConfiguration> allJobConfigurations = this.jobConfigurationRepository.getAllJobConfigurations(this.springBatchLightminConfigurationProperties.getApplicationName());
        for (final JobConfiguration jobConfiguration : allJobConfigurations) {
            try {
                this.jobConfigurationRepository.delete(jobConfiguration,
                        this.springBatchLightminConfigurationProperties.getApplicationName());
            } catch (final NoSuchJobConfigurationException e) {
                fail(e.getMessage());
            }
        }
    }

}
