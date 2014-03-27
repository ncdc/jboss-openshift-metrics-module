package com.openshift.metrics.extension;

import static com.openshift.metrics.extension.Constants.METRIC_SOURCES;
import static com.openshift.metrics.extension.Constants.MODEL_CONTROLLER_CLIENT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Service that handles scheduling jobs that gather and publish metrics.
 *
 * @author Andy Goldstein <agoldste@redhat.com>
 */
public class MetricsService implements Service<MetricsService> {
    private final Logger log = Logger.getLogger(MetricsService.class);

    private Scheduler scheduler;

    private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();

    private ModelControllerClient modelControllerClient;

    private ExecutorService managementOperationExecutor;

    public MetricsService() {
        try {
            // I originally had this in start() but it looks like start() and
            // the various ADD operations that are invoked after the subsystem
            // is parsed can run in separate threads, so I had a race condition
            // where something like createJob() would run before start(), resulting
            // in an NPE because the scheduler was null. Not sure of the best way
            // to do this...
            log.debug("Creating metrics scheduler");
            scheduler = StdSchedulerFactory.getDefaultScheduler();

            // Is this the most appropriate executor type?
            managementOperationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable run) {
                    Thread thread = new Thread(run);
                    thread.setName("Metrics Management Client Thread");
                    thread.setDaemon(true);
                    return thread;
                }
            });
        } catch (SchedulerException e) {
            //TODO better error handling
            e.printStackTrace();
        }
    }

    /**
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public MetricsService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        try {
            log.debug("Creating model controller client");
            modelControllerClient = injectedModelController.getValue().createClient(managementOperationExecutor);

            log.debug("Adding model controller client to scheduler context");
            scheduler.getContext().put(MODEL_CONTROLLER_CLIENT, modelControllerClient);

            log.info("Starting metrics scheduler");
            scheduler.start();
        } catch(SchedulerException e) {
            throw new StartException("Error starting scheduler", e);
        }
    }

    /**
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        try {
            // is this the correct shutdown order?
            log.info("Shutting down metrics scheduler");
            scheduler.shutdown();

            log.info("Shutting down model controller client executor");
            managementOperationExecutor.shutdown();
        } catch (SchedulerException e) {
            //TODO log error
        }
    }

    /**
     * Get this service's {@link ServiceName} for the MSC
     *
     * @return this service's {@link ServiceName}
     */
    public static ServiceName getServiceName() {
        return ServiceName.JBOSS.append(Constants.METRICS_SERVICE_NAME);
    }

    /**
     * Create an empty job for the given cron schedule
     *
     * @param schedule a cron expression
     * @param enabled TODO
     * @return the created {@link JobDetail}
     * @throws SchedulerException
     */
    public JobDetail createJob(String schedule, boolean enabled) throws SchedulerException {
        log.infov("Creating metrics group job [schedule={0}, enabled={1}]", schedule, enabled);
        JobDetail job = JobBuilder.newJob(MetricJob.class)
                                  .withIdentity(schedule)
                                  .storeDurably()
                                  .build();

        job.getJobDataMap().put(Constants.ENABLED, enabled);

        scheduler.addJob(job, false);
        return job;
    }

    /**
     * Enables the job with the given cron schedule
     * @param schedule a cron expression
     * @throws SchedulerException
     */
    public void enableJob(String schedule) throws SchedulerException {
        log.infov("Enabling metrics group job [schedule={0}]", schedule);
        final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
        scheduleJob(schedule, job);
    }

    /**
     * Disables the job with the given cron schedule
     * @param schedule a cron expression
     * @throws SchedulerException
     */
    public void disableJob(String schedule) throws SchedulerException {
        log.infov("Disabling metrics group job [schedule={0}]", schedule);
        scheduler.unscheduleJob(TriggerKey.triggerKey(schedule));
    }

    /**
     * Remove a job for the given schedule
     *
     * @param schedule a cron expression
     * @throws SchedulerException
     */
    public void removeJob(String schedule) throws SchedulerException {
        log.infov("Removing metrics group job [schedule={0}]", schedule);
        scheduler.deleteJob(JobKey.jobKey(schedule));
    }

    /**
     * Add a metric source to the job for the given schedule
     *
     * @param schedule a cron expression
     * @param source a path to either a {@link ResourceDefinition} or an MBean
     * @throws SchedulerException
     */
    public void addMetricSource(String schedule, Source source) throws SchedulerException {
        log.infov("Adding metrics source [schedule={0}, type={1}, source={2}, enabled={3}]", schedule, source.getType(), source.getPath(), source.isEnabled());
        final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        if(null == metricSourceMap) {
            metricSourceMap = new HashMap<String, Source>();
            jobDataMap.put(METRIC_SOURCES, metricSourceMap);
        }
        metricSourceMap.put(source.getPath(), source);
        scheduler.addJob(job, true);
    }

    public void enableMetricSource(String schedule, String sourcePath, boolean enabled) throws SchedulerException {
        log.infov("{0} metrics source [schedule={1}, source={2}]", enabled ? "Enabling" : "Disabling", schedule, sourcePath);
        final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        final Source source = metricSourceMap.get(sourcePath);
        source.setEnabled(enabled);
        scheduler.addJob(job, true);
    }

    /**
     * Remove a metric source from the job for the given schedule
     * @param schedule a cron expression
     * @param source a path to either a {@link ResourceDefinition} or an MBean
     * @throws SchedulerException
     */
    public void removeMetricSource(String schedule, String source) throws SchedulerException {
        log.infov("Removing metrics source [schedule={0}, source={1}]", schedule, source);
        final JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        metricSourceMap.remove(source);
        scheduler.addJob(job, true);
    }

    /**
     * Add a metric to the job for the given schedule and source
     *
     * @param schedule a cron expression
     * @param sourcePath a path to either a {@link ResourceDefinition} or an MBean
     * @param sourceKey name of an attribute for the source to look up
     * @param publishKey name to use when publishing this metric
     * @param enabled TODO
     * @throws SchedulerException
     */
    public void addMetric(String schedule, String sourcePath, String sourceKey, String publishKey, boolean enabled) throws SchedulerException {
        log.infov("Adding metric [schedule={0}, source={1}, sourceKey={2}, publishKey={3}, enabled={4}]", schedule, sourcePath, sourceKey, publishKey, enabled);
        JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));

        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        final Source source = metricSourceMap.get(sourcePath);
        source.addMetric(sourceKey, publishKey, enabled);

        scheduler.addJob(job, true);

        final boolean jobEnabled = jobDataMap.getBoolean(Constants.ENABLED);

        if(jobEnabled && !scheduler.checkExists(TriggerKey.triggerKey(schedule))) {
            scheduleJob(schedule, job);
        }
    }

    public void enableMetric(String schedule, String sourcePath, String publishKey, boolean enabled) throws SchedulerException {
        log.infov("{0} metric [schedule={1}, source={2}, publishKey={3}]", enabled ? "Enabling" : "Disabling", schedule, sourcePath, publishKey);
        JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));

        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        final Source source = metricSourceMap.get(sourcePath);

        source.enableMetric(publishKey, enabled);

        scheduler.addJob(job, true);
    }

    /**
     * Remove a metric from the job for the given schedule and source
     *
     * @param schedule a cron expression
     * @param sourcePath a path to either a {@link ResourceDefinition} or an MBean
     * @param publishKey name to use when publishing this metric
     * @throws SchedulerException
     */
    public void removeMetric(String schedule, String sourcePath, String publishKey) throws SchedulerException {
        log.infov("Adding metric [schedule={0}, source={1}, publishKey={2}]", schedule, sourcePath, publishKey);
        JobDetail job = scheduler.getJobDetail(JobKey.jobKey(schedule));
        final JobDataMap jobDataMap = job.getJobDataMap();
        @SuppressWarnings("unchecked")
        Map<String, Source> metricSourceMap = (Map<String, Source>) jobDataMap.get(METRIC_SOURCES);
        final Source source = metricSourceMap.get(sourcePath);
        source.removeMetric(publishKey);

        scheduler.addJob(job, true);
    }

    private void scheduleJob(String schedule, JobDetail job) throws SchedulerException {
        Trigger trigger = createTrigger(job, schedule);
        scheduler.scheduleJob(trigger);
    }

    private Trigger createTrigger(JobDetail job, final String cronExpression) {
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(cronExpression))
                .forJob(job)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
        return trigger;
    }

    /**
     * Get the model controller
     * @return the model controller
     */
    public InjectedValue<ModelController> getInjectedModelController() {
        return injectedModelController;
    }
}
